package no.entur.logging.cloud.gcp.spring.grpc.lognet;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.grpc.Status;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.appender.scope.predicate.HigherOrEqualToLogLevelPredicate;
import no.entur.logging.cloud.appender.scope.predicate.LoggerNamePrefixHigherOrEqualToLogLevelPredicate;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.properties.OndemandFailure;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.properties.OndemandGrpcTrigger;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.properties.OndemandLogLevelTrigger;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.properties.OndemandPath;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.properties.OndemandProperties;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.properties.OndemandSuccess;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.properties.ServiceMatcherConfiguration;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.scope.GrpcContextLoggingScopeFactory;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.scope.GrpcLoggingScopeContextInterceptor;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.scope.GrpcLoggingScopeFilter;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.scope.GrpcLoggingScopeFilters;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcAddMdcTraceToResponseInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcTraceMdcContextInterceptor;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(value = {OndemandProperties.class})
public class LognetLoggingAutoConfiguration {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LognetLoggingAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(GrpcTraceMdcContextInterceptor.class)
    public GrpcTraceMdcContextInterceptor grpcTraceMdcContextInterceptor() {
        return GrpcTraceMdcContextInterceptor.newBuilder().build();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcAddMdcTraceToResponseInterceptor.class)
    public GrpcAddMdcTraceToResponseInterceptor grpcAddMdcTraceToResponseInterceptor() {
        return new GrpcAddMdcTraceToResponseInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcMdcContextInterceptor.class)
    public GrpcMdcContextInterceptor grpcMdcContextInterceptor() {
        return GrpcMdcContextInterceptor.newBuilder().build();
    }

    @Configuration
    @ConditionalOnProperty(name = {"entur.logging.grpc.ondemand.enabled"}, havingValue = "true", matchIfMissing = false)
    public static class OndemandConfiguration {

        @Bean
        @ConditionalOnMissingBean(GrpcLoggingScopeContextInterceptor.class)
        public GrpcLoggingScopeContextInterceptor grpcLoggingScopeContextInterceptor(OndemandProperties properties) {
            LoggingScopeAsyncAppender appender = getLoggingScopeAsyncAppender();

            LOGGER.info("Configure on-demand GRPC logging");

            GrpcLoggingScopeFilters filters = new GrpcLoggingScopeFilters();

            GrpcLoggingScopeFilter defaultFilter = toFilter(properties.getSuccess(), properties.getFailure());
            filters.setDefaultFilter(defaultFilter);

            List<OndemandPath> paths = properties.getPaths();
            for (OndemandPath matcher : paths) {
                if(!matcher.isEnabled()) {
                    continue;
                }
                GrpcLoggingScopeFilter filter = toFilter(matcher.getSuccess(), matcher.getFailure());

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

            appender.setLoggingScopeFactory(grpcContextLoggingScopeFactory);

            GrpcLoggingScopeContextInterceptor interceptor = GrpcLoggingScopeContextInterceptor
                    .newBuilder()
                    .withAppender(appender)
                    .withFilters(filters)
                    .build();

            return interceptor;
        }

        protected Level toLevel(String level) {
            switch (level) {
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

        private static LoggingScopeAsyncAppender getLoggingScopeAsyncAppender() {
            Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
            if(!appenderIterator.hasNext()) {
                throw new IllegalStateException("No appenders configured, expected at least one of type " + LoggingScopeAsyncAppender.class.getName());
            }
            while (appenderIterator.hasNext()) {
                Appender<ILoggingEvent> appender = appenderIterator.next();
                if (appender instanceof LoggingScopeAsyncAppender) {
                    return (LoggingScopeAsyncAppender) appender;
                }
            }
            throw new IllegalStateException("Expected appender type " + LoggingScopeAsyncAppender.class.getName());
        }


        public GrpcLoggingScopeFilter toFilter(OndemandSuccess success, OndemandFailure failure) {
            GrpcLoggingScopeFilter filter = new GrpcLoggingScopeFilter();

            Level alwaysLogLevel = toLevel(success.getLevel());
            filter.setQueuePredicate( (e) -> e.getLevel().toInt() < alwaysLogLevel.toInt());

            Level optionallyLogLevel = toLevel(failure.getLevel());
            filter.setIgnorePredicate( (e) -> e.getLevel().toInt() < optionallyLogLevel.toInt());

            OndemandGrpcTrigger httpStatusCodeTrigger = failure.getGrpc();
            if(httpStatusCodeTrigger.isEnabled()) {
                Set<String> status = httpStatusCodeTrigger.getStatus().stream().map( (s) -> s.toUpperCase()).collect(Collectors.toSet());

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
                    filter.setLogLevelFailurePredicate(new HigherOrEqualToLogLevelPredicate(flushForLevel.toInt()));
                } else {
                    filter.setLogLevelFailurePredicate(new LoggerNamePrefixHigherOrEqualToLogLevelPredicate(flushForLevel.toInt(), name));
                }
            } else {
                filter.setLogLevelFailurePredicate((e) -> false);
            }

            return filter;
        }
    }
}
