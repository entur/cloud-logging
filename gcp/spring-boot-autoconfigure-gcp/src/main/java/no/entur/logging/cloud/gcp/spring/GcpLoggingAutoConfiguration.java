package no.entur.logging.cloud.gcp.spring;


import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.gcp.micrometer.StackdriverLogbackMetrics;
import no.entur.logging.cloud.gcp.spring.ondemand.ConditionalOnDisabledOndemandLogging;
import no.entur.logging.cloud.gcp.spring.ondemand.ConditionalOnEnabledOndemandLogging;
import no.entur.logging.cloud.gcp.spring.ondemand.GcpOndemandLoggingMeterBinder;
import no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.micrometer.metrics.autoconfigure.logging.logback.LogbackMetricsAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Configure metrics. Normally this can be done via turbo filters in logback.
 *
 * However for on-demand-logging, the turbo filter does not know whether the log statement will be
 * written or not. So disable turbo filters for on-demand logging, and rather connect
 * metrics within the appender so that only actually written log statements will count in our own metrics.
 *
 */
@AutoConfiguration(before = {LogbackMetricsAutoConfiguration.class})

@Configuration
public class GcpLoggingAutoConfiguration {

    @Bean
    @ConditionalOnClass(StackdriverLogbackMetrics.class)
    @ConditionalOnDisabledOndemandLogging
    public LogbackMetrics stackdriverLogbackMetrics() {
        return new StackdriverLogbackMetrics();
    }

    @Bean
    @ConditionalOnClass(no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics.class)
    @ConditionalOnDisabledOndemandLogging
    public LogbackMetrics logbackMetrics() {
        return new no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics();
    }

    @Bean
    @ConditionalOnClass({DevOpsLogbackMetrics.class, StackdriverLogbackMetrics.class})
    @ConditionalOnEnabledOndemandLogging
    public MeterBinder ondemandMeterBinder() {
        // make sure the log events which are not logged are not included in our own metrics
        GcpOndemandLoggingMeterBinder binder = new GcpOndemandLoggingMeterBinder();

        LoggingScopeAsyncAppender appender = LoggingScopeAsyncAppender.get();

        appender.setMetrics(binder);

        return binder;
    }

    /**
     * Suppresses Spring Boot's built-in {@code LogbackMetrics} when on-demand logging is enabled.
     * In on-demand mode metrics are counted by {@code GcpOndemandLoggingMeterBinder} (appender-side)
     * rather than by a turbo filter, so the built-in turbo-filter-based counting must be disabled.
     */

    @Bean
    @ConditionalOnClass({DevOpsLogbackMetrics.class, StackdriverLogbackMetrics.class})
    @ConditionalOnEnabledOndemandLogging
    public LogbackMetrics gcpOndemandLogbackMetricsSuppressor() {
        return new LogbackMetrics() {
            @Override
            public void bindTo(io.micrometer.core.instrument.MeterRegistry registry) {
                // no-op: on-demand mode uses GcpOndemandLoggingMeterBinder for metrics
            }
        };
    }

}
