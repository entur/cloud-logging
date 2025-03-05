package no.entur.logging.cloud.azure.spring.ondemand;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import no.entur.logging.cloud.appender.scope.LoggingScopeMetrics;
import no.entur.logging.cloud.azure.micrometer.AzureMetricsTurboFilter;
import no.entur.logging.cloud.micrometer.DevOpsMetricsTurboFilter;
import no.entur.logging.cloud.micrometer.LoggingEventMetrics;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 *
 * Class for enabling metrics for logging events which are actually logged.
 *
 */

public class AzureOndemandLoggingMeterBinder implements MeterBinder, LoggingScopeMetrics {

    private final List<LoggingEventMetrics> filters = new ArrayList<>();

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        filters.add(new DevOpsMetricsTurboFilter(meterRegistry, emptyList()));
        filters.add(new AzureMetricsTurboFilter(meterRegistry, emptyList()));
    }

    @Override
    public void increment(ILoggingEvent iLoggingEvent) {
        for (LoggingEventMetrics filter : filters) {
            filter.increment(iLoggingEvent);
        }
    }


}
