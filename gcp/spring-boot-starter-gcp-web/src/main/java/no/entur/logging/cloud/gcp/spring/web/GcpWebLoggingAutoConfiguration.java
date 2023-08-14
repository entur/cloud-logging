package no.entur.logging.cloud.gcp.spring.web;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import jakarta.servlet.http.HttpServletResponse;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.gcp.micrometer.StackdriverLogbackMetrics;
import no.entur.logging.cloud.gcp.spring.web.scope.ThreadLocalLoggingScopeFactory;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;
import java.util.function.Predicate;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.REQUEST;

@Configuration
@EnableConfigurationProperties(value = {OndemandProperties.class})
public class GcpWebLoggingAutoConfiguration {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(GcpWebLoggingAutoConfiguration.class);

    @Bean
    @ConditionalOnClass(StackdriverLogbackMetrics.class)
    public LogbackMetrics stackdriverLogbackMetrics() {
        return new StackdriverLogbackMetrics();
    }

    @Bean
    @ConditionalOnClass(no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics.class)
    public LogbackMetrics logbackMetrics() {
        return new no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics();
    }

    @Configuration
    @ConditionalOnProperty(name = {"entur.logging.http.ondemand.enabled"}, havingValue = "true", matchIfMissing = false)
    public static class OndemandConfiguration {

        @Bean
        @ConditionalOnMissingBean(OndemandLoggerPredicate.class)
        public OndemandLoggerPredicate ondemandLoggerPredicate() {
            return (loggerName) -> true;
        }

        @Bean
        public FilterRegistrationBean<OndemandFilter> ondemandFilter(OndemandProperties properties, OndemandLoggerPredicate loggerPredicate) {
            LoggingScopeAsyncAppender appender = getLoggingScopeAsyncAppender();

            Level level = toLevel(properties.getLevel());

            LOGGER.info("Configure on-demand HTTP logging with level " + level);

            int limit = level.toInt();

            Predicate<ILoggingEvent> predicate = (e) -> e.getLevel().levelInt < limit && loggerPredicate.test(e.getLoggerName());

            ThreadLocalLoggingScopeFactory threadLocalLoggingScopeFactory = new ThreadLocalLoggingScopeFactory();
            threadLocalLoggingScopeFactory.setFilter(predicate);

            appender.setLoggingScopeFactory(threadLocalLoggingScopeFactory);

            OndemandFilter filter = new OndemandFilter(appender, (response) -> {
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                return httpServletResponse.getStatus() >= 400;
            });

            FilterRegistrationBean<OndemandFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.setDispatcherTypes(REQUEST, ERROR);

            registration.setOrder(properties.getOrder());
            registration.addUrlPatterns("/*");
            return registration;
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
