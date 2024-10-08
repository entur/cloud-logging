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
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandFailure;
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandHttpHeader;
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandHttpResponseTrigger;
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandHttpStatus;
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandLogLevelTrigger;
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandPath;
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandProperties;
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandSuccess;
import no.entur.logging.cloud.spring.ondemand.web.properties.OndemandTroubleshoot;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpHeaderPresentPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilters;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpStatusAtLeastOrNotPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpStatusAtLeastPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpStatusEqualToPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpStatusNotEqualToPredicate;
import no.entur.logging.cloud.spring.ondemand.web.scope.ThreadLocalLoggingScopeFactory;
import org.slf4j.LoggerFactory;
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
import java.util.Set;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.REQUEST;

@Configuration
@EnableConfigurationProperties(value = {OndemandProperties.class})
public class GcpWebOndemandLoggingAutoConfiguration {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(GcpWebOndemandLoggingAutoConfiguration.class);

    @Configuration
    @ConditionalOnProperty(name = {"entur.logging.http.ondemand.enabled"}, havingValue = "true", matchIfMissing = false)
    public static class OndemandConfiguration {

        @Bean
        public FilterRegistrationBean<OndemandFilter> ondemandFilter(OndemandProperties properties) {
            LoggingScopeAsyncAppender appender = getAppender();

            LOGGER.info("Configure on-demand HTTP logging ");

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

            ThreadLocalLoggingScopeFactory factory = new ThreadLocalLoggingScopeFactory();

            OndemandFilter filter = new OndemandFilter(appender, filters, factory);

            appender.addScopeProvider(factory);

            FilterRegistrationBean<OndemandFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.setDispatcherTypes(REQUEST, ERROR);

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

            List<OndemandHttpHeader> headers = troubleshoot.getHttp().getHeaders();
            if(headers != null) {
                Set<String> headerNames = new HashSet<>(); // thread safe for reading
                for (OndemandHttpHeader header : headers) {
                    if(header.isEnabled()) {
                        headerNames.add(header.getName().toLowerCase());
                    }
                }
                if(!headerNames.isEmpty()) {
                    filter.setHttpHeaderPresentPredicate(new HttpHeaderPresentPredicate(headerNames));
                } else {
                    filter.setHttpHeaderPresentPredicate((e) -> false);
                }
            } else {
                filter.setHttpHeaderPresentPredicate((e) -> false);
            }

            Level alwaysLogLevel = toLevel(success.getLevel());
            filter.setQueuePredicate( (e) -> e.getLevel().toInt() < alwaysLogLevel.toInt());

            Level optionallyLogLevel = toLevel(failure.getLevel());
            filter.setIgnorePredicate( (e) -> e.getLevel().toInt() < optionallyLogLevel.toInt());

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
