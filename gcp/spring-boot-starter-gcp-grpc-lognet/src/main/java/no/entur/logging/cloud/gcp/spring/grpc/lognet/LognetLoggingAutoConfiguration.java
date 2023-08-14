package no.entur.logging.cloud.gcp.spring.grpc.lognet;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextInterceptor;
import no.entur.logging.cloud.grpc.mdc.scope.GrpcContextLoggingScopeFactory;
import no.entur.logging.cloud.grpc.mdc.scope.GrpcLoggingScopeContextInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcAddMdcTraceToResponseInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcTraceMdcContextInterceptor;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;
import java.util.function.Predicate;

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
        @ConditionalOnMissingBean(OndemandLoggerPredicate.class)
        public OndemandLoggerPredicate grpcLoggingScopePredicate() {
            return (loggerName) -> true;
        }

        @Bean
        @ConditionalOnMissingBean(GrpcLoggingScopeContextInterceptor.class)
        public GrpcLoggingScopeContextInterceptor grpcLoggingScopeContextInterceptor(OndemandProperties properties, OndemandLoggerPredicate loggingScopeLoggerPredicate) {
            LoggingScopeAsyncAppender appender = getLoggingScopeAsyncAppender();

            Level level = toLevel(properties.getLevel());

            LOGGER.info("Configure on-demand GRPC logging with level " + level);

            int limit = level.toInt();

            Predicate<ILoggingEvent> predicate = (e) -> e.getLevel().levelInt < limit && loggingScopeLoggerPredicate.test(e.getLoggerName());

            GrpcContextLoggingScopeFactory grpcContextLoggingScopeFactory = new GrpcContextLoggingScopeFactory();
            grpcContextLoggingScopeFactory.setFilter(predicate);

            appender.setLoggingScopeFactory(grpcContextLoggingScopeFactory);

            GrpcLoggingScopeContextInterceptor interceptor = GrpcLoggingScopeContextInterceptor
                    .newBuilder()
                    .withAppender(appender)
                    .withPredicate((status) -> !status.isOk())
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
    }
}
