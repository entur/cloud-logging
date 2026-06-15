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
import no.entur.logging.cloud.micrometer.LoggingEventMetrics;
import org.slf4j.Marker;

import java.util.List;

public class StackdriverMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    protected final CompatibleCounter alertCount;
    protected final CompatibleCounter criticalCount;
    protected final CompatibleCounter errorCount;
    protected final CompatibleCounter warnCount;
    protected final CompatibleCounter infoCount;
    protected final CompatibleCounter debugCount;
    protected final CompatibleCounter defaultCount;

    public StackdriverMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        // emergency level is not in use

        alertCount = CompatibleCounter.register("logback.gcp.events", registry, tags,
                "severity", "alert",
                "Number of alert severity events that made it to the logs");

        criticalCount = CompatibleCounter.register("logback.gcp.events", registry, tags,
                "severity", "critical",
                "Number of critical severity events that made it to the logs");

        errorCount = CompatibleCounter.register("logback.gcp.events", registry, tags,
                "severity", "error",
                "Number of error severity events that made it to the logs");

        warnCount = CompatibleCounter.register("logback.gcp.events", registry, tags,
                "severity", "warning",
                "Number of warn severity events that made it to the logs");

        infoCount = CompatibleCounter.register("logback.gcp.events", registry, tags,
                "severity", "info",
                "Number of info severity events that made it to the logs");

        debugCount = CompatibleCounter.register("logback.gcp.events", registry, tags,
                "severity", "debug",
                "Number of debug severity events that made it to the logs");

        defaultCount = CompatibleCounter.register("logback.gcp.events", registry, tags,
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
        // cannot use logger.isEnabledFor(level), as it would cause a StackOverflowError by calling this filter again!
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

    public void increment(DevOpsLevel severity, int amount) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                alertCount.add(amount);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                criticalCount.add(amount);
                break;
            }
            case WARN: {
                warnCount.add(amount);
                break;
            }
            case INFO: {
                infoCount.add(amount);
                break;
            }
            case DEBUG: {
                debugCount.add(amount);
                break;
            }
            case TRACE: {
                defaultCount.add(amount);
                break;
            }
            case ERROR_TELL_ME_TOMORROW:
            default: {
                errorCount.add(amount);
                break;
            }
        }
    }
}

