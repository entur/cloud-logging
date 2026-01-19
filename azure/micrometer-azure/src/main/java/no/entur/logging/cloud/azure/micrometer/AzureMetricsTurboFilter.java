package no.entur.logging.cloud.azure.micrometer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.slf4j.Marker;
import no.entur.logging.cloud.micrometer.LoggingEventMetrics;

import java.util.List;


public class AzureMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    private final Counter alertCounter;
    private final Counter criticalCounter;
    private final Counter errorCounter;
    private final Counter warnCounter;
    private final Counter infoCounter;
    private final Counter debugCounter;
    private final Counter defaultCounter;

    public AzureMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        // emergency level is not in use

        // TODO doe it make sense to use these levels when they are not supported by the log accumulation tool?
        alertCounter = Counter.builder("logback.azure.events")
                .tags(tags).tags("severity", "alert")
                .description("Number of alert severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        criticalCounter = Counter.builder("logback.azure.events")
                .tags(tags).tags("severity", "critical")
                .description("Number of critical severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        errorCounter = Counter.builder("logback.azure.events")
                .tags(tags).tags("severity", "error")
                .description("Number of error severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        warnCounter = Counter.builder("logback.azure.events")
                .tags(tags).tags("severity", "warning")
                .description("Number of warn severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        infoCounter = Counter.builder("logback.azure.events")
                .tags(tags).tags("severity", "info")
                .description("Number of info severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        debugCounter = Counter.builder("logback.azure.events")
                .tags(tags).tags("severity", "debug")
                .description("Number of debug severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        defaultCounter = Counter.builder("logback.azure.events")
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
                        errorCounter.increment();
                    }
                } else {
                    errorCounter.increment();
                }

                break;
            case Level.WARN_INT:
                warnCounter.increment();
                break;
            case Level.INFO_INT:
                infoCounter.increment();
                break;
            case Level.DEBUG_INT:
                debugCounter.increment();
                break;
            case Level.TRACE_INT:
                defaultCounter.increment();
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
                        errorCounter.increment();
                    }
                } else {
                    errorCounter.increment();
                }

                break;
            case Level.WARN_INT:
                warnCounter.increment();
                break;
            case Level.INFO_INT:
                infoCounter.increment();
                break;
            case Level.DEBUG_INT:
                debugCounter.increment();
                break;
            case Level.TRACE_INT:
                defaultCounter.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(DevOpsLevel severity) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                alertCounter.increment();
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                criticalCounter.increment();
                break;
            }
            case WARN: {
                warnCounter.increment();
                break;
            }
            case INFO: {
                infoCounter.increment();
                break;
            }
            case DEBUG: {
                debugCounter.increment();
                break;
            }
            case TRACE: {
                defaultCounter.increment();
                break;
            }
            case ERROR_TELL_ME_TOMORROW:
            default: {
                errorCounter.increment();
                break;
            }
        }
    }
}
