package no.entur.logging.cloud.azure.micrometer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.slf4j.Marker;
import no.entur.logging.cloud.micrometer.LoggingEventMetrics;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;


public class AzureMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    private final LongAdder alertCount;
    private final LongAdder criticalCount;
    private final LongAdder errorCount;
    private final LongAdder warnCount;
    private final LongAdder infoCount;
    private final LongAdder debugCount;
    private final LongAdder defaultCount;

    public AzureMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        // emergency level is not in use

        alertCount = new LongAdder();
        FunctionCounter.builder("logback.azure.events", alertCount, LongAdder::doubleValue)
                .tags(tags).tags("severity", "alert")
                .description("Number of alert severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        criticalCount = new LongAdder();
        FunctionCounter.builder("logback.azure.events", criticalCount, LongAdder::doubleValue)
                .tags(tags).tags("severity", "critical")
                .description("Number of critical severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        errorCount = new LongAdder();
        FunctionCounter.builder("logback.azure.events", errorCount, LongAdder::doubleValue)
                .tags(tags).tags("severity", "error")
                .description("Number of error severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        warnCount = new LongAdder();
        FunctionCounter.builder("logback.azure.events", warnCount, LongAdder::doubleValue)
                .tags(tags).tags("severity", "warning")
                .description("Number of warn severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        infoCount = new LongAdder();
        FunctionCounter.builder("logback.azure.events", infoCount, LongAdder::doubleValue)
                .tags(tags).tags("severity", "info")
                .description("Number of info severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        debugCount = new LongAdder();
        FunctionCounter.builder("logback.azure.events", debugCount, LongAdder::doubleValue)
                .tags(tags).tags("severity", "debug")
                .description("Number of debug severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        defaultCount = new LongAdder();
        FunctionCounter.builder("logback.azure.events", defaultCount, LongAdder::doubleValue)
                .tags(tags).tags("severity", "default")
                .description("Number of default severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        // cannot use logger.isEnabledFor(level), as it would cause a StackOverflowError by calling this filter again!
        if (level.isGreaterOrEqual(logger.getEffectiveLevel()) && format != null) {
            increment(marker, level);
        }

        return FilterReply.NEUTRAL;
    }

    public void increment(ILoggingEvent event) {
        Level level = event.getLevel();
        switch (level.toInt()) {
            case Level.ERROR_INT:

                List<Marker> markerList = event.getMarkerList();
                if(markerList != null && !markerList.isEmpty()) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(markerList);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorCount.increment();
                    }
                } else {
                    errorCount.increment();
                }

                break;
            case Level.WARN_INT:
                warnCount.increment();
                break;
            case Level.INFO_INT:
                infoCount.increment();
                break;
            case Level.DEBUG_INT:
                debugCount.increment();
                break;
            case Level.TRACE_INT:
                defaultCount.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(Marker marker, Level level) {
        switch (level.toInt()) {
            case Level.ERROR_INT:

                if (marker != null) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(marker);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorCount.increment();
                    }
                } else {
                    errorCount.increment();
                }

                break;
            case Level.WARN_INT:
                warnCount.increment();
                break;
            case Level.INFO_INT:
                infoCount.increment();
                break;
            case Level.DEBUG_INT:
                debugCount.increment();
                break;
            case Level.TRACE_INT:
                defaultCount.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(DevOpsLevel severity) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                alertCount.increment();
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                criticalCount.increment();
                break;
            }
            case WARN: {
                warnCount.increment();
                break;
            }
            case INFO: {
                infoCount.increment();
                break;
            }
            case DEBUG: {
                debugCount.increment();
                break;
            }
            case TRACE: {
                defaultCount.increment();
                break;
            }
            case ERROR_TELL_ME_TOMORROW:
            default: {
                errorCount.increment();
                break;
            }
        }
    }
}
