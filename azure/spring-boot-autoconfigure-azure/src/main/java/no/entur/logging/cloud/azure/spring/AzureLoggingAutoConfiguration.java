package no.entur.logging.cloud.azure.spring;


import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.azure.micrometer.AzureLogbackMetrics;
import no.entur.logging.cloud.azure.spring.ondemand.AzureOndemandLoggingMeterBinder;
import no.entur.logging.cloud.gcp.spring.ondemand.ConditionalOnDisabledOndemandLogging;
import no.entur.logging.cloud.gcp.spring.ondemand.ConditionalOnEnabledOndemandLogging;
import no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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


@Configuration
public class AzureLoggingAutoConfiguration {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(AzureLoggingAutoConfiguration.class);

    @Bean
    @ConditionalOnClass(AzureLogbackMetrics.class)
    @ConditionalOnDisabledOndemandLogging
    public LogbackMetrics azureLogbackMetrics() {
        return new AzureLogbackMetrics();
    }

    @Bean
    @ConditionalOnDisabledOndemandLogging
    @ConditionalOnClass(no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics.class)
    public LogbackMetrics logbackMetrics() {
        return new no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics();
    }

    @Bean
    @ConditionalOnClass({DevOpsLogbackMetrics.class, AzureLogbackMetrics.class})
    @ConditionalOnEnabledOndemandLogging
    public MeterBinder ondemandMeterBinder() {
        // make sure the log events which are not logged are not included in our own metrics
        AzureOndemandLoggingMeterBinder binder = new AzureOndemandLoggingMeterBinder();

        LoggingScopeAsyncAppender appender = LoggingScopeAsyncAppender.get();

        appender.setMetrics(binder);

        return binder;
    }
}
