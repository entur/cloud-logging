package no.entur.logging.cloud.micrometer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.slf4j.Marker;

import java.util.List;

public class DevOpsMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    protected final Counter errorWakeMeUpRightNowCounter;
    protected final Counter errorInterruptMyDinnerCounter;
    protected final Counter errorTellMeTomorrowCounter;

    protected final Counter errorCounter; // alias for all errors / backwards compatibility

    protected final Counter warnCounter;
    protected final Counter infoCounter;
    protected final Counter debugCounter;
    protected final Counter traceCounter;

    public DevOpsMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        // DevOps-specific level values are never registered by Micrometer's built-in
        // MetricsTurboFilter, so a plain Counter.builder().register() is always safe here.
        errorWakeMeUpRightNowCounter = Counter.builder("logback.events")
                .tags(tags).tags("level", "errorWakeMeUpRightNow")
                .description("Number of error 'Wake Me Up Right Now' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        errorInterruptMyDinnerCounter = Counter.builder("logback.events")
                .tags(tags).tags("level", "errorInterruptMyDinner")
                .description("Number of error 'Interrupt My Dinner' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        errorTellMeTomorrowCounter = Counter.builder("logback.events")
                .tags(tags).tags("level", "errorTellMeTomorrow")
                .description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        // Standard level values (error, warn, info, debug, trace) are also used by
        // Micrometer's built-in MetricsTurboFilter.  In Micrometer 1.17+ that filter
        // registers FunctionCounter + LongAdder for these IDs instead of a plain Counter.
        // Calling Counter.builder().register() when a FunctionCounter already occupies
        // the same ID throws IllegalArgumentException.  captureOrRegister() checks the
        // registry first: it reuses an existing Counter, returns NOOP_COUNTER when any
        // other meter type is present, and only registers a new Counter when the slot is empty.
        errorCounter = captureOrRegister(registry, "logback.events", tags, "level", "error",
                "Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)");

        warnCounter = captureOrRegister(registry, "logback.events", tags, "level", "warn",
                "Number of warn level events that made it to the logs");

        infoCounter = captureOrRegister(registry, "logback.events", tags, "level", "info",
                "Number of info level events that made it to the logs");

        debugCounter = captureOrRegister(registry, "logback.events", tags, "level", "debug",
                "Number of debug level events that made it to the logs");

        traceCounter = captureOrRegister(registry, "logback.events", tags, "level", "trace",
                "Number of trace level events that made it to the logs");
    }

    /**
     * Captures an existing {@link Counter} from the registry under {@code name + tags + tagKey=tagValue},
     * or registers a fresh one when the slot is empty.
     *
     * <p>When a non-{@code Counter} meter (e.g., a {@code FunctionCounter}) already occupies
     * that ID, it is removed from the registry so that a new, incrementable {@link Counter}
     * can be registered in its place.  This guarantees that the returned meter can always be
     * used to increment the underlying counter.</p>
     */
    private static Counter captureOrRegister(MeterRegistry registry, String name,
            Iterable<Tag> baseTags, String tagKey, String tagValue, String description) {
        Meter existing = registry.find(name).tag(tagKey, tagValue).tags(baseTags).meter();
        if (existing instanceof Counter c) {
            return c;
        }
        if (existing != null) {
            throw new IllegalStateException("Meter with name '" + name + "' and tag '" + tagKey + "=" + tagValue
                    + "' already exists but is not a Counter: " + existing.getClass().getName());
        }
        return Counter.builder(name)
                .tags(baseTags)
                .tag(tagKey, tagValue)
                .description(description)
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
                errorCounter.increment();

                List<Marker> markerList = event.getMarkerList();
                if(markerList != null && !markerList.isEmpty()) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(markerList);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorTellMeTomorrowCounter.increment();
                    }
                } else {
                    errorTellMeTomorrowCounter.increment();
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
                traceCounter.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    public void increment(Marker marker, Level level) {
        switch (level.toInt()) {
            case Level.ERROR_INT:
                errorCounter.increment();

                if (marker != null) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(marker);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorTellMeTomorrowCounter.increment();
                    }
                } else {
                    errorTellMeTomorrowCounter.increment();
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
                traceCounter.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(DevOpsLevel severity) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                errorWakeMeUpRightNowCounter.increment();
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                errorInterruptMyDinnerCounter.increment();
                break;
            }
            case ERROR_TELL_ME_TOMORROW: {
                errorTellMeTomorrowCounter.increment();
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
                traceCounter.increment();
                break;
            }
            default: {
                errorTellMeTomorrowCounter.increment();
            }
        }
    }


    public void increment(DevOpsLevel severity, int amount) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                errorWakeMeUpRightNowCounter.increment(amount);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                errorInterruptMyDinnerCounter.increment(amount);
                break;
            }
            case ERROR_TELL_ME_TOMORROW: {
                errorTellMeTomorrowCounter.increment(amount);
                break;
            }
            case WARN: {
                warnCounter.increment(amount);
                break;
            }
            case INFO: {
                infoCounter.increment(amount);
                break;
            }
            case DEBUG: {
                debugCounter.increment(amount);
                break;
            }
            case TRACE: {
                traceCounter.increment(amount);
                break;
            }
            default: {
                errorTellMeTomorrowCounter.increment(amount);
            }
        }
    }
}
