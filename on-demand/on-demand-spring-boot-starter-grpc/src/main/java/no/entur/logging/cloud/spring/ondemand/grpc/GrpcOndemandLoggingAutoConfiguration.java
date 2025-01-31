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

            GrpcContextLoggingScopeFactory grpcContextLoggingScopeFactory = new GrpcContextLoggingScopeFactory();

            GrpcLoggingScopeContextInterceptor interceptor = GrpcLoggingScopeContextInterceptor
                    .newBuilder()
                    .withSink(appender)
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

            Level alwaysLogLevel = toLevel(success.getLevel());
            filter.setQueuePredicate( (e) -> e.getLevel().toInt() < alwaysLogLevel.toInt());

            Level optionallyLogLevel = toLevel(failure.getLevel());
            filter.setIgnorePredicate( (e) -> e.getLevel().toInt() < optionallyLogLevel.toInt());

            OndemandGrpcRequestTrigger troubleshootHttpRequestTrigger = troubleshoot.getGrpc();

            Set<String> troubleshootHeaderNames = troubleshoot.getGrpc().toEnabledHeaderNames();
            if(troubleshootHttpRequestTrigger.isEnabled() && !troubleshootHeaderNames.isEmpty()) {
                filter.setGrpcHeaderPresentPredicate(new GrpcHeaderPresentPredicate(troubleshootHeaderNames));

                LOGGER.info("Configure GRPC {} troubleshooting {} logging for headers {}", methodNames.isEmpty() ? serviceName : serviceName + methodNames, troubleshoot.getLevel(), troubleshootHeaderNames);

                // troubleshooting: to take effect, the troubleshooting must be
                //  - lower than success, and/or
                //  - lower than failure
                Level troubleShootAlwaysLogLevel = toLevel(troubleshoot.getLevel());
                if(troubleShootAlwaysLogLevel.toInt() > alwaysLogLevel.toInt()) {
                    filter.setTroubleshootQueuePredicate( (e) -> e.getLevel().toInt() < alwaysLogLevel.toInt());
                } else {
                    filter.setTroubleshootQueuePredicate( (e) -> e.getLevel().toInt() < troubleShootAlwaysLogLevel.toInt());
                }

                Level troubleShootOptionallyLogLevel = troubleShootAlwaysLogLevel;
                if( troubleShootOptionallyLogLevel.toInt() > optionallyLogLevel.toInt()) {
                    filter.setTroubleshootIgnorePredicate( (e) -> e.getLevel().toInt() < optionallyLogLevel.toInt());
                } else {
                    filter.setTroubleshootIgnorePredicate( (e) -> e.getLevel().toInt() < troubleShootOptionallyLogLevel.toInt());
                }

            } else {
                filter.setGrpcHeaderPresentPredicate((e) -> false);
                filter.setTroubleshootQueuePredicate((e) -> false);
                filter.setTroubleshootIgnorePredicate((e) -> false);
            }

            OndemandGrpcResponseTrigger httpStatusCodeTrigger = failure.getGrpc();
            if(httpStatusCodeTrigger.isEnabled()) {
                Set<String> status = httpStatusCodeTrigger.getStatus().stream().map( (s) -> s.toUpperCase()).collect(Collectors.toSet());

                LOGGER.info("Configure GRPC  {} on-demand logging for status codes ", methodNames.isEmpty() ? serviceName : serviceName + methodNames, status);

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
