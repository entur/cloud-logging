package no.entur.logging.cloud.gcp.spring.ondemand;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import no.entur.logging.cloud.appender.scope.LoggingScopeMetrics;
import no.entur.logging.cloud.gcp.micrometer.StackdriverMetricsTurboFilter;
import no.entur.logging.cloud.micrometer.LoggingEventMetrics;
import no.entur.logging.cloud.micrometer.DevOpsMetricsTurboFilter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 *
 * Class for enabling metrics for logging events which are actually logged.
 *
 */

public class GcpOndemandLoggingMeterBinder implements MeterBinder, LoggingScopeMetrics {

    private final List<LoggingEventMetrics> filters = new ArrayList<>();

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        filters.add(new DevOpsMetricsTurboFilter(meterRegistry, emptyList()));
        filters.add(new StackdriverMetricsTurboFilter(meterRegistry, emptyList()));
    }

    @Override
    public void increment(ILoggingEvent iLoggingEvent) {
        for (LoggingEventMetrics filter : filters) {
            filter.increment(iLoggingEvent);
        }
    }


}
