package no.entur.logging.cloud.gcp.micrometer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import no.entur.logging.cloud.micrometer.CompatibleCounter;
import no.entur.logging.cloud.micrometer.CompatibleCounterFactory;
import no.entur.logging.cloud.micrometer.LoggingEventMetrics;
import org.slf4j.Marker;

import java.util.List;

public class StackdriverMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    private static final CompatibleCounterFactory COUNTER_FACTORY = CompatibleCounterFactory.forCurrentSpringBootVersion();

    protected final CompatibleCounter alertCount;
    protected final CompatibleCounter criticalCount;
    protected final CompatibleCounter errorCount;
    protected final CompatibleCounter warnCount;
    protected final CompatibleCounter infoCount;
    protected final CompatibleCounter debugCount;
    protected final CompatibleCounter defaultCount;

    public StackdriverMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        // emergency level is not in use

        alertCount = COUNTER_FACTORY.register("logback.gcp.events", registry, tags,
                "severity", "alert",
                "Number of alert severity events that made it to the logs");

        criticalCount = COUNTER_FACTORY.register("logback.gcp.events", registry, tags,
                "severity", "critical",
                "Number of critical severity events that made it to the logs");

        errorCount = COUNTER_FACTORY.register("logback.gcp.events", registry, tags,
                "severity", "error",
                "Number of error severity events that made it to the logs");

        warnCount = COUNTER_FACTORY.register("logback.gcp.events", registry, tags,
                "severity", "warning",
                "Number of warn severity events that made it to the logs");

        infoCount = COUNTER_FACTORY.register("logback.gcp.events", registry, tags,
                "severity", "info",
                "Number of info severity events that made it to the logs");

        debugCount = COUNTER_FACTORY.register("logback.gcp.events", registry, tags,
                "severity", "debug",
                "Number of debug severity events that made it to the logs");

        defaultCount = COUNTER_FACTORY.register("logback.gcp.events", registry, tags,
                "severity", "default",
                "Number of default severity events that made it to the logs");
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if(level.isGreaterOrEqual(logger.getEffectiveLevel()) && format != null) {
            increment(marker, level);
        }

        return FilterReply.NEUTRAL;
    }

    public void increment(ILoggingEvent event) {
        // cannot use logger.isEnabledFor(level), as it would cause a StackOverflowError by calling this filter again!
        Level level = event.getLevel();
        switch (level.toInt()) {
            case Level.ERROR_INT:

                List<Marker> markerList = event.getMarkerList();
                if(markerList != null && !markerList.isEmpty()) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(markerList);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorCount.accept(1L);
                    }
                } else {
                    errorCount.accept(1L);
                }

                break;
            case Level.WARN_INT:
                warnCount.accept(1L);
                break;
            case Level.INFO_INT:
                infoCount.accept(1L);
                break;
            case Level.DEBUG_INT:
                debugCount.accept(1L);
                break;
            case Level.TRACE_INT:
                defaultCount.accept(1L);
                break;
            default: {
                // do nothing
            }
        }

   }

   protected void increment(Marker marker, Level level) {
        // cannot use logger.isEnabledFor(level), as it would cause a StackOverflowError by calling this filter again!
        switch (level.toInt()) {
            case Level.ERROR_INT:

                if (marker != null) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(marker);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorCount.accept(1L);
                    }
                } else {
                    errorCount.accept(1L);
                }

                break;
            case Level.WARN_INT:
                warnCount.accept(1L);
                break;
            case Level.INFO_INT:
                infoCount.accept(1L);
                break;
            case Level.DEBUG_INT:
                debugCount.accept(1L);
                break;
            case Level.TRACE_INT:
                defaultCount.accept(1L);
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(DevOpsLevel severity) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                alertCount.accept(1L);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                criticalCount.accept(1L);
                break;
            }
            case WARN: {
                warnCount.accept(1L);
                break;
            }
            case INFO: {
                infoCount.accept(1L);
                break;
            }
            case DEBUG: {
                debugCount.accept(1L);
                break;
            }
            case TRACE: {
                defaultCount.accept(1L);
                break;
            }
            case ERROR_TELL_ME_TOMORROW:
            default: {
                errorCount.accept(1L);
                break;
            }
        }
    }

    public void increment(DevOpsLevel severity, int amount) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                alertCount.accept(amount);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                criticalCount.accept(amount);
                break;
            }
            case WARN: {
                warnCount.accept(amount);
                break;
            }
            case INFO: {
                infoCount.accept(amount);
                break;
            }
            case DEBUG: {
                debugCount.accept(amount);
                break;
            }
            case TRACE: {
                defaultCount.accept(amount);
                break;
            }
            case ERROR_TELL_ME_TOMORROW:
            default: {
                errorCount.accept(amount);
                break;
            }
        }
    }
}

