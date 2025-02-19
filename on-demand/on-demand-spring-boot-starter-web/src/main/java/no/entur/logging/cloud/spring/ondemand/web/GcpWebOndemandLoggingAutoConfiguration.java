package no.entur.logging.cloud.spring.ondemand.web;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.appender.scope.predicate.HigherOrEqualToLogLevelPredicate;
import no.entur.logging.cloud.appender.scope.predicate.LoggerNamePrefixHigherOrEqualToLogLevelPredicate;
import no.entur.logging.cloud.spring.ondemand.web.properties.*;
import no.entur.logging.cloud.spring.ondemand.web.scope.*;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpHeaderPresentPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilters;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpStatusAtLeastOrNotPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpStatusAtLeastPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpStatusEqualToPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpStatusNotEqualToPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.ThreadLocalLoggingScopeFactory;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.REQUEST;
import static jakarta.servlet.DispatcherType.ASYNC;

@Configuration
@EnableConfigurationProperties(value = {OndemandProperties.class})
public class GcpWebOndemandLoggingAutoConfiguration {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(GcpWebOndemandLoggingAutoConfiguration.class);

    @Configuration
    @ConditionalOnProperty(name = {"entur.logging.http.ondemand.enabled"}, havingValue = "true", matchIfMissing = false)
    public static class OndemandConfiguration {

        private final ThreadLocalLoggingScopeFactory factory;
        private OndemandProperties properties;

        public OndemandConfiguration(OndemandProperties properties) {
            this.properties = properties;
            this.factory = new ThreadLocalLoggingScopeFactory(properties.getFlushMode());
        }

        @Bean
        public LoggingScopeControls loggingScopeControls() {
            return factory;
        }
        
        @Bean
        @ConditionalOnMissingBean(LoggingScopeThreadUtils.class)
        public LoggingScopeThreadUtils loggingScopeThreadUtils(OndemandProperties properties) {
            return new DefaultLoggingScopeThreadUtils(factory);
        }

        @Bean
        public FilterRegistrationBean<OndemandFilter> ondemandFilter() {
            LoggingScopeAsyncAppender appender = getAppender();

            LOGGER.info("Enable on-demand HTTP logging filter");

            HttpLoggingScopeFilters filters = new HttpLoggingScopeFilters();

            HttpLoggingScopeFilter defaultFilter = toFilter(null, properties.getSuccess(), properties.getFailure(), properties.getTroubleshoot());
            filters.setDefaultFilter(defaultFilter);

            List<OndemandPath> paths = properties.getPaths();
            for (OndemandPath path : paths) {
                if(!path.isEnabled()) {
                    continue;
                }
                HttpLoggingScopeFilter filter = toFilter(path.getMatcher(), path.getSuccess(), path.getFailure(), path.getTroubleshoot());
                RequestMatcher requestMatcher = AntPathRequestMatcher.antMatcher(path.getMatcher());
                filters.addFilter(requestMatcher, filter);
            }

            OndemandFilter filter = new OndemandFilter(appender, filters, factory);

            appender.addScopeProvider(factory);

            FilterRegistrationBean<OndemandFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.setDispatcherTypes(REQUEST, ERROR, ASYNC);

            registration.setOrder(properties.getFilterOrder());
            registration.addUrlPatterns(properties.getFilterUrlPatterns());
            return registration;
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

        public HttpLoggingScopeFilter toFilter(String matcher, OndemandSuccess success, OndemandFailure failure, OndemandTroubleshoot troubleshoot) {
            HttpLoggingScopeFilter filter = new HttpLoggingScopeFilter();

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

            OndemandHttpRequestTrigger troubleshootHttpRequestTrigger = troubleshoot.getHttp();

            Set<String> troubleshootHeaderNames = troubleshoot.getHttp().toEnabledHeaderNames();
            if(troubleshootHttpRequestTrigger.isEnabled() && !troubleshootHeaderNames.isEmpty()) {
                filter.setHttpHeaderPresentPredicate(new HttpHeaderPresentPredicate(troubleshootHeaderNames));

                LOGGER.info("Configure {} troubleshooting {} logging for headers {}", matcher == null ? "default" : matcher, troubleshoot.getLevel().toString(), troubleshootHeaderNames);

                // troubleshooting: lower the success / failure level
                Level troubleShootLevel = toLevel(troubleshoot.getLevel());
                int successLevelWhenTroubleShooting = Math.min(successLevel.toInt(), troubleShootLevel.toInt());
                filter.setTroubleshootQueuePredicate((e) -> e.getLevel().toInt() < successLevelWhenTroubleShooting);

                int failureLevelWhenTroubleShooting = Math.min(failureLevel.toInt(), troubleShootLevel.toInt());
                filter.setTroubleshootIgnorePredicate((e) -> e.getLevel().toInt() < failureLevelWhenTroubleShooting);
            } else {
                filter.setTroubleshootQueuePredicate((e) -> false);
                filter.setTroubleshootIgnorePredicate((e) -> false);
                filter.setHttpHeaderPresentPredicate((e) -> false);
            }

            OndemandHttpResponseTrigger httpStatusCodeTrigger = failure.getHttp();
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

            OndemandDurationTrigger duration = failure.getDuration();
            if(duration.isEnabled() && duration.getAfter() != null) {
                Duration after = duration.getAfter();

                filter.setFailureDuration(after.toMillis());

                LOGGER.info("Configure on-demand logging for {} http exchanges longer than {}ms ", matcher == null ? "default" : matcher, filter.getFailureDuration());
            }

            OndemandLogLevelTrigger logLevelTrigger = failure.getLogger();
            if(logLevelTrigger.isEnabled()) {
                Level flushForLevel = toLevel(logLevelTrigger.getLevel());
                List<String> name = logLevelTrigger.getName();
                if(name.isEmpty()) {
                    LOGGER.info("Configure {} on-demand logging for log statements with at least {} severity", matcher == null ? "default" : matcher, flushForLevel.toString());

                    filter.setLogLevelFailurePredicate(new HigherOrEqualToLogLevelPredicate(flushForLevel.toInt()));
                } else {
                    LOGGER.info("Configure {} on-demand logging for {} log statements with at least {} severity ", name, matcher == null ? "default" : matcher, flushForLevel.toString());

                    filter.setLogLevelFailurePredicate(new LoggerNamePrefixHigherOrEqualToLogLevelPredicate(flushForLevel.toInt(), name));
                }
            } else {
                filter.setLogLevelFailurePredicate(null);
            }

            return filter;
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

    }


    @Configuration
    @ConditionalOnProperty(name = {"entur.logging.http.ondemand.enabled"}, havingValue = "false", matchIfMissing = true)
    public static class DisabledOndemandConfiguration {

        // this avoids breaking autowiring when on-demand is disabled
        @Bean
        @ConditionalOnMissingBean(LoggingScopeThreadUtils.class)
        public LoggingScopeThreadUtils loggingScopeThreadUtils() {
            return new NoopLoggingScopeThreadUtils();
        }

        @Bean
        @ConditionalOnMissingBean(LoggingScopeControls.class)
        public LoggingScopeControls noopLoggingScopeControls() {
            return new NoopLoggingScopeControls();
        }
    }

}
