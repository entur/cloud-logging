package no.entur.logging.cloud.gcp.micrometer;

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
import no.entur.logging.cloud.micrometer.LoggingEventMetrics;
import org.slf4j.Marker;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;

public class StackdriverMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    protected final LongAdder alertAdder = new LongAdder();
    protected final LongAdder criticalAdder = new LongAdder();
    protected final LongAdder errorAdder = new LongAdder();
    protected final LongAdder warnAdder = new LongAdder();
    protected final LongAdder infoAdder = new LongAdder();
    protected final LongAdder debugAdder = new LongAdder();
    protected final LongAdder defaultAdder = new LongAdder();

    public StackdriverMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        // emergency level is not in use

        // TODO: Legacy Counter-based registration was replaced with FunctionCounter + LongAdder.
        //       This comment can be deleted once the migration is confirmed complete.
        FunctionCounter.builder("logback.gcp.events", alertAdder, LongAdder::doubleValue)
                .tags(tags).tags("severity", "alert")
                .description("Number of alert severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        FunctionCounter.builder("logback.gcp.events", criticalAdder, LongAdder::doubleValue)
                .tags(tags).tags("severity", "critical")
                .description("Number of critical severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        FunctionCounter.builder("logback.gcp.events", errorAdder, LongAdder::doubleValue)
                .tags(tags).tags("severity", "error")
                .description("Number of error severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        FunctionCounter.builder("logback.gcp.events", warnAdder, LongAdder::doubleValue)
                .tags(tags).tags("severity", "warning")
                .description("Number of warn severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        FunctionCounter.builder("logback.gcp.events", infoAdder, LongAdder::doubleValue)
                .tags(tags).tags("severity", "info")
                .description("Number of info severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        FunctionCounter.builder("logback.gcp.events", debugAdder, LongAdder::doubleValue)
                .tags(tags).tags("severity", "debug")
                .description("Number of debug severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        FunctionCounter.builder("logback.gcp.events", defaultAdder, LongAdder::doubleValue)
                .tags(tags).tags("severity", "default")
                .description("Number of default severity events that made it to the logs")
                .baseUnit("events")
                .register(registry);
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
                        errorAdder.increment();
                    }
                } else {
                    errorAdder.increment();
                }

                break;
            case Level.WARN_INT:
                warnAdder.increment();
                break;
            case Level.INFO_INT:
                infoAdder.increment();
                break;
            case Level.DEBUG_INT:
                debugAdder.increment();
                break;
            case Level.TRACE_INT:
                defaultAdder.increment();
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
                        errorAdder.increment();
                    }
                } else {
                    errorAdder.increment();
                }

                break;
            case Level.WARN_INT:
                warnAdder.increment();
                break;
            case Level.INFO_INT:
                infoAdder.increment();
                break;
            case Level.DEBUG_INT:
                debugAdder.increment();
                break;
            case Level.TRACE_INT:
                defaultAdder.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(DevOpsLevel severity) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                alertAdder.increment();
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                criticalAdder.increment();
                break;
            }
            case WARN: {
                warnAdder.increment();
                break;
            }
            case INFO: {
                infoAdder.increment();
                break;
            }
            case DEBUG: {
                debugAdder.increment();
                break;
            }
            case TRACE: {
                defaultAdder.increment();
                break;
            }
            case ERROR_TELL_ME_TOMORROW:
            default: {
                errorAdder.increment();
                break;
            }
        }
    }

    public void increment(DevOpsLevel severity, int amount) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                alertAdder.add(amount);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                criticalAdder.add(amount);
                break;
            }
            case WARN: {
                warnAdder.add(amount);
                break;
            }
            case INFO: {
                infoAdder.add(amount);
                break;
            }
            case DEBUG: {
                debugAdder.add(amount);
                break;
            }
            case TRACE: {
                defaultAdder.add(amount);
                break;
            }
            case ERROR_TELL_ME_TOMORROW:
            default: {
                errorAdder.add(amount);
                break;
            }
        }
    }
}
