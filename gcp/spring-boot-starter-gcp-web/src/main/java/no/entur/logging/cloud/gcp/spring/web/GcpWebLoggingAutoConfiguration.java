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
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandHttpStatus;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandHttpTrigger;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandLogLevelTrigger;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandPath;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandProperties;
import no.entur.logging.cloud.gcp.spring.web.properties.OndemandSuccess;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpStatusAtLeastOrNotPredicate;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpStatusAtLeastPredicate;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpLoggingScopeFilters;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpStatusEqualToPredicate;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpStatusNotEqualToPredicate;
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
            
            HttpLoggingScopeFilter defaultFilter = toFilter(null, properties.getSuccess(), properties.getFailure());
            filters.setDefaultFilter(defaultFilter);

            List<OndemandPath> paths = properties.getPaths();
            for (OndemandPath path : paths) {
                if(!path.isEnabled()) {
                    continue;
                }
                HttpLoggingScopeFilter filter = toFilter(path.getMatcher(), path.getSuccess(), path.getFailure());
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

        public HttpLoggingScopeFilter toFilter(String matcher, OndemandSuccess success, OndemandFailure failure) {
            HttpLoggingScopeFilter filter = new HttpLoggingScopeFilter();

            Level alwaysLogLevel = toLevel(success.getLevel());
            filter.setQueuePredicate( (e) -> e.getLevel().toInt() < alwaysLogLevel.toInt());

            Level optionallyLogLevel = toLevel(failure.getLevel());
            filter.setIgnorePredicate( (e) -> e.getLevel().toInt() < optionallyLogLevel.toInt());

            OndemandHttpTrigger httpStatusCodeTrigger = failure.getHttp();
            if(httpStatusCodeTrigger.isEnabled()) {
                OndemandHttpStatus statusCode = httpStatusCodeTrigger.getStatusCode();

                int equalOrHigherThan = statusCode.getEqualOrHigherThan();
                List<Integer> notEqualTo = statusCode.getNotEqualTo();
                List<Integer> equalTo = statusCode.getEqualTo();

                if(!notEqualTo.isEmpty() && !equalTo.isEmpty()) {
                    throw new IllegalStateException("Status code equal-to cannot be combined with not-equal-to");
                }

                // if equal-or-higher-to is configured, both equal-to and not-equal-to applies to the
                // low or high range of the equal-or-higher-to limit

                if(equalOrHigherThan == -1 && notEqualTo.isEmpty() && !equalTo.isEmpty()) {
                    // only equal to
                    filter.setHttpStatusFailurePredicate(new HttpStatusEqualToPredicate(equalTo));

                    LOGGER.info("Configure {} on-demand logging for status codes equal to " + equalTo, matcher == null ? "default" : matcher);
                } else if(equalOrHigherThan == -1 && equalTo.isEmpty() && !notEqualTo.isEmpty()) {
                    // only not equal to
                    LOGGER.info("Configure {} on-demand logging for status codes not equal to " + notEqualTo, matcher == null ? "default" : matcher);
                    filter.setHttpStatusFailurePredicate(new HttpStatusNotEqualToPredicate(notEqualTo));
                } else if(equalOrHigherThan != -1 && equalTo.isEmpty() && notEqualTo.isEmpty()) {
                    // only higher than
                    LOGGER.info("Configure {} on-demand logging for status codes at least " + equalOrHigherThan, matcher == null ? "default" : matcher);

                    filter.setHttpStatusFailurePredicate(new HttpStatusAtLeastPredicate(equalOrHigherThan));
                } else if(equalOrHigherThan != -1) {

                    for (Integer integer : notEqualTo) {
                        if(integer < equalOrHigherThan) {
                            throw new IllegalStateException("Not-equal-to " + integer + " is already included in higher-or-equal-to " + equalOrHigherThan);
                        }
                    }

                    for (Integer integer : equalTo) {
                        if(integer >= equalOrHigherThan) {
                            throw new IllegalStateException("Equal-to " + integer + " is already included in higher-or-equal-to " + equalOrHigherThan);
                        }
                    }

                    LOGGER.info("Configure {} on-demand logging for status codes at least " + equalOrHigherThan + ", excluding " + notEqualTo + " or including " + equalTo + ")", matcher == null ? "default" : matcher);

                    filter.setHttpStatusFailurePredicate(new HttpStatusAtLeastOrNotPredicate(statusCode.getEqualOrHigherThan(), equalTo, notEqualTo));
                } else {
                    filter.setHttpStatusFailurePredicate((e) -> false);
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
