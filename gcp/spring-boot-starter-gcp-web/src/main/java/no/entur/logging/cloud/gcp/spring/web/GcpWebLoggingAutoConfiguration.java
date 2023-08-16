package no.entur.logging.cloud.gcp.spring.web;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.appender.scope.predicate.HigherOrEqualToLogLevelPredicate;
import no.entur.logging.cloud.appender.scope.predicate.LoggerNamePrefixHigherOrEqualToLogLevelPredicate;
import no.entur.logging.cloud.gcp.micrometer.StackdriverLogbackMetrics;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandFailure;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandHttpStatusCode;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandHttpTrigger;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandLogLevelTrigger;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandPath;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandProperties;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandSuccess;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpStatusAtLeastOrExcludePredicate;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpStatusAtLeastPredicate;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpLoggingScopeFilters;
import no.entur.logging.cloud.gcp.spring.web.scope.ThreadLocalLoggingScopeFactory;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
        public FilterRegistrationBean<OndemandFilter> ondemandFilter(OndemandProperties properties) {
            LoggingScopeAsyncAppender appender = getLoggingScopeAsyncAppender();

            LOGGER.info("Configure on-demand HTTP logging ");

            HttpLoggingScopeFilters filters = new HttpLoggingScopeFilters();
            
            HttpLoggingScopeFilter defaultFilter = toFilter(properties.getSuccess(), properties.getFailure());
            filters.setDefaultFilter(defaultFilter);

            List<OndemandPath> paths = properties.getPaths();
            for (OndemandPath path : paths) {
                if(!path.isEnabled()) {
                    continue;
                }
                HttpLoggingScopeFilter filter = toFilter(path.getSuccess(), path.getFailure());
                RequestMatcher requestMatcher = AntPathRequestMatcher.antMatcher(path.getMatcher());
                filters.addFilter(requestMatcher, filter);
            }

            ThreadLocalLoggingScopeFactory threadLocalLoggingScopeFactory = new ThreadLocalLoggingScopeFactory();

            appender.setLoggingScopeFactory(threadLocalLoggingScopeFactory);

            OndemandFilter filter = new OndemandFilter(appender, filters);

            FilterRegistrationBean<OndemandFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.setDispatcherTypes(REQUEST, ERROR);

            registration.setOrder(properties.getFilterOrder());
            registration.addUrlPatterns(properties.getFilterUrlPatterns());
            return registration;
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

        public HttpLoggingScopeFilter toFilter(OndemandSuccess success, OndemandFailure failure) {
            HttpLoggingScopeFilter filter = new HttpLoggingScopeFilter();

            Level alwaysLogLevel = toLevel(success.getLevel());
            filter.setQueuePredicate( (e) -> e.getLevel().toInt() < alwaysLogLevel.toInt());

            Level optionallyLogLevel = toLevel(failure.getLevel());
            filter.setIgnorePredicate( (e) -> e.getLevel().toInt() < optionallyLogLevel.toInt());

            OndemandHttpTrigger httpStatusCodeTrigger = failure.getHttp();
            if(httpStatusCodeTrigger.isEnabled()) {
                OndemandHttpStatusCode statusCode = httpStatusCodeTrigger.getStatusCode();

                List<Integer> except = statusCode.getExcept();
                if(except.isEmpty()) {
                    filter.setHttpStatusFailurePredicate(new HttpStatusAtLeastPredicate(statusCode.getEqualOrHigherThan()));
                } else {
                    filter.setHttpStatusFailurePredicate(new HttpStatusAtLeastOrExcludePredicate(statusCode.getEqualOrHigherThan(), new HashSet<>(except)));
                }
            } else {
                filter.setHttpStatusFailurePredicate((e) -> false);
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

    }
}
