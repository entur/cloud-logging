package no.entur.logging.cloud.spring.ondemand.grpc;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.grpc.Status;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.appender.scope.predicate.HigherOrEqualToLogLevelPredicate;
import no.entur.logging.cloud.appender.scope.predicate.LoggerNamePrefixHigherOrEqualToLogLevelPredicate;
import no.entur.logging.cloud.spring.ondemand.grpc.properties.*;
import no.entur.logging.cloud.spring.ondemand.grpc.scope.GrpcContextLoggingScopeFactory;
import no.entur.logging.cloud.spring.ondemand.grpc.scope.GrpcHeaderPresentPredicate;
import no.entur.logging.cloud.spring.ondemand.grpc.scope.GrpcLoggingScopeContextInterceptor;
import no.entur.logging.cloud.spring.ondemand.grpc.scope.GrpcLoggingScopeFilter;
import no.entur.logging.cloud.spring.ondemand.grpc.scope.GrpcLoggingScopeFilters;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(value = {OndemandProperties.class})
public class GrpcOndemandLoggingAutoConfiguration {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(GrpcOndemandLoggingAutoConfiguration.class);

    @Configuration
    @ConditionalOnProperty(name = {"entur.logging.grpc.ondemand.enabled"}, havingValue = "true", matchIfMissing = false)
    public static class OndemandConfiguration {

        @Bean
        @ConditionalOnMissingBean(GrpcLoggingScopeContextInterceptor.class)
        public GrpcLoggingScopeContextInterceptor grpcLoggingScopeContextInterceptor(OndemandProperties properties) {
            LoggingScopeAsyncAppender appender = getAppender();

            LOGGER.info("Configure on-demand GRPC logging");

            GrpcLoggingScopeFilters filters = new GrpcLoggingScopeFilters();

            GrpcLoggingScopeFilter defaultFilter = toFilter("default", Collections.emptyList(), properties.getSuccess(), properties.getFailure(), properties.getTroubleshoot());
            filters.setDefaultFilter(defaultFilter);

            List<OndemandPath> paths = properties.getPaths();
            for (OndemandPath matcher : paths) {
                if(!matcher.isEnabled()) {
                    continue;
                }
                GrpcLoggingScopeFilter filter = toFilter(matcher.getServiceName(), matcher.getMethodNames(), matcher.getSuccess(), matcher.getFailure(), matcher.getTroubleshoot());

                String serviceName = matcher.getServiceName();
                List<String> methodNames = matcher.getMethodNames();
                if(methodNames.isEmpty()) {
                    for (String methodName : methodNames) {
                        filters.addFilter(serviceName, methodName, filter);
                    }
                } else {
                    filters.addFilter(serviceName, filter);
                }
            }

            GrpcContextLoggingScopeFactory grpcContextLoggingScopeFactory = new GrpcContextLoggingScopeFactory(properties.getFlushMode(), appender);

            GrpcLoggingScopeContextInterceptor interceptor = GrpcLoggingScopeContextInterceptor
                    .newBuilder()
                    .withFilters(filters)
                    .withOrder(properties.getInterceptorOrder())
                    .withFactory(grpcContextLoggingScopeFactory)
                    .build();

            appender.addScopeProvider(grpcContextLoggingScopeFactory);

            return interceptor;
        }

        protected Level toLevel(String level) {
            switch (level.toLowerCase()) {
                case ("trace"):
                    return Level.TRACE;
                case ("debug"):
                    return Level.DEBUG;
                case ("info"):
                    return Level.INFO;
                case ("warn"):
                    return Level.WARN;
                case ("error"):
                    return Level.ERROR;
                default:
                    throw new IllegalStateException("Level [" + level + "] not recognized.");
            }
        }

        private static LoggingScopeAsyncAppender getAppender() {
            Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
            if(!appenderIterator.hasNext()) {
                throw new IllegalStateException("No on-demand log appenders configured, expected at least one which is implementing " + LoggingScopeAsyncAppender.class.getName());
            }
            while (appenderIterator.hasNext()) {
                Appender<ILoggingEvent> appender = appenderIterator.next();
                if (appender instanceof LoggingScopeAsyncAppender) {
                    return (LoggingScopeAsyncAppender) appender;
                }
            }
            throw new IllegalStateException("Expected on-demand log appender implementing " + LoggingScopeAsyncAppender.class.getName());
        }

        public GrpcLoggingScopeFilter toFilter(String serviceName, List<String> methodNames, OndemandSuccess success, OndemandFailure failure, OndemandTroubleshoot troubleshoot) {
            GrpcLoggingScopeFilter filter = new GrpcLoggingScopeFilter();

            Level successLevel = toLevel(success.getLevel());
            Level failureLevel = toLevel(failure.getLevel());
            if(failureLevel.toInt() >= successLevel.toInt()) {
                throw new IllegalStateException("Expected on-demand logging failure level < success level. In other words, more logging when failure.");
            }

            // approach: check whether to ignore log event first, then check whether to queue or not (if not, print the statement at once).

            // i.e. if even is not logged in a failure, it is never logged
            filter.setIgnorePredicate((e) -> e.getLevel().toInt() < failureLevel.toInt());

            // if not ignored, do we want to queue the log event?
            filter.setQueuePredicate((e) -> e.getLevel().toInt() < successLevel.toInt());

            OndemandGrpcRequestTrigger troubleshootHttpRequestTrigger = troubleshoot.getGrpc();

            Set<String> troubleshootHeaderNames = troubleshoot.getGrpc().toEnabledHeaderNames();
            if(troubleshootHttpRequestTrigger.isEnabled() && !troubleshootHeaderNames.isEmpty()) {
                filter.setGrpcHeaderPresentPredicate(new GrpcHeaderPresentPredicate(troubleshootHeaderNames));

                LOGGER.info("Configure GRPC {} troubleshooting {} logging for headers {}", methodNames.isEmpty() ? serviceName : serviceName + methodNames, troubleshoot.getLevel(), troubleshootHeaderNames);

                // troubleshooting: lower the success / failure level
                Level troubleShootLevel = toLevel(troubleshoot.getLevel());
                int successLevelWhenTroubleShooting = Math.min(successLevel.toInt(), troubleShootLevel.toInt());
                filter.setTroubleshootQueuePredicate((e) -> e.getLevel().toInt() < successLevelWhenTroubleShooting);

                int failureLevelWhenTroubleShooting = Math.min(failureLevel.toInt(), troubleShootLevel.toInt());
                filter.setTroubleshootIgnorePredicate((e) -> e.getLevel().toInt() < failureLevelWhenTroubleShooting);
            } else {
                filter.setGrpcHeaderPresentPredicate((e) -> false);
                filter.setTroubleshootQueuePredicate((e) -> false);
                filter.setTroubleshootIgnorePredicate((e) -> false);
            }

            OndemandDurationTrigger duration = failure.getDuration();
            if(duration.isEnabled()) {
                Duration before = duration.getBefore();
                Duration after = duration.getAfter();

                boolean hasBefore = before != null;
                boolean hasAfter = after != null;

                if(hasBefore && hasAfter) {
                    long beforeMillis = before.toMillis();
                    long afterMillis = after.toMillis();

                    if(beforeMillis > afterMillis) {
                        //  assume interval [after, before] => failure
                        LOGGER.info("Configure {} on-demand logging for gRPC exchanges with duration in interval {}ms to {}ms", methodNames.isEmpty() ? serviceName : serviceName + methodNames, beforeMillis, afterMillis);
                    } else {
                        //  assume intervals [0, before] or [after, infinite] => failure
                        LOGGER.info("Configure {} on-demand logging for gRPC exchanges longer than {}ms or shorter than {}ms", methodNames.isEmpty() ? serviceName : serviceName + methodNames, afterMillis, beforeMillis);
                    }
                } else if(hasBefore) {
                    LOGGER.info("Configure {} on-demand logging for gRPC exchanges shorter than {}ms ", methodNames.isEmpty() ? serviceName : serviceName + methodNames, before.toMillis());
                } else if(hasAfter) {
                    LOGGER.info("Configure {} on-demand logging for gRPC exchanges longer than {}ms ", methodNames.isEmpty() ? serviceName : serviceName + methodNames, after.toMillis());
                }
            }

            OndemandGrpcResponseTrigger httpStatusCodeTrigger = failure.getGrpc();
            if(httpStatusCodeTrigger.isEnabled()) {
                Set<String> status = httpStatusCodeTrigger.getStatus().stream().map( (s) -> s.toUpperCase()).collect(Collectors.toSet());

                LOGGER.info("Configure GRPC  {} on-demand logging for status codes {}", methodNames.isEmpty() ? serviceName : serviceName + methodNames, status);

                Status.Code[] values = Status.Code.values();

                boolean[] codes = new boolean[values.length];
                for(int i = 0; i < codes.length; i++) {
                    codes[i] = status.contains(values[i].name());
                }

                filter.setGrpcStatusPredicate((e) -> codes[e.getCode().ordinal()]);
            } else {
                filter.setGrpcStatusPredicate((e) -> false);
            }

            OndemandLogLevelTrigger logLevelTrigger = failure.getLogger();
            if(logLevelTrigger.isEnabled()) {
                Level flushForLevel = toLevel(logLevelTrigger.getLevel());
                List<String> name = logLevelTrigger.getName();
                if(name.isEmpty()) {
                    LOGGER.info("Configure GRPC {} on-demand logging for log statements with at least {} severity", methodNames.isEmpty() ? serviceName : serviceName + methodNames, flushForLevel);

                    filter.setLogLevelFailurePredicate(new HigherOrEqualToLogLevelPredicate(flushForLevel.toInt()));
                } else {
                    LOGGER.info("Configure GRPC {} on-demand logging for {} log statements with at least {} severity ", name, methodNames.isEmpty() ? serviceName : serviceName + methodNames, flushForLevel);

                    filter.setLogLevelFailurePredicate(new LoggerNamePrefixHigherOrEqualToLogLevelPredicate(flushForLevel.toInt(), name));
                }
            } else {
                filter.setLogLevelFailurePredicate(null);
            }

            return filter;
        }

    }
}
