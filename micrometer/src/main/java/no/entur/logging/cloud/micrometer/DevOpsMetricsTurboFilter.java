package no.entur.logging.cloud.micrometer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.slf4j.Marker;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;

public class DevOpsMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    protected final LongAdder errorWakeMeUpRightNowAdder = new LongAdder();
    protected final LongAdder errorInterruptMyDinnerAdder = new LongAdder();
    protected final LongAdder errorTellMeTomorrowAdder = new LongAdder();

    protected final LongAdder errorAdder = new LongAdder(); // alias for all errors / backwards compatibility

    protected final LongAdder warnAdder = new LongAdder();
    protected final LongAdder infoAdder = new LongAdder();
    protected final LongAdder debugAdder = new LongAdder();
    protected final LongAdder traceAdder = new LongAdder();

    public DevOpsMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        // DevOps-specific level values are never registered by Micrometer's built-in
        // MetricsTurboFilter, so a plain FunctionCounter registration is always safe here.
        FunctionCounter.builder("logback.events", errorWakeMeUpRightNowAdder, LongAdder::doubleValue)
                .tags(tags).tags("level", "errorWakeMeUpRightNow")
                .description("Number of error 'Wake Me Up Right Now' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        FunctionCounter.builder("logback.events", errorInterruptMyDinnerAdder, LongAdder::doubleValue)
                .tags(tags).tags("level", "errorInterruptMyDinner")
                .description("Number of error 'Interrupt My Dinner' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        FunctionCounter.builder("logback.events", errorTellMeTomorrowAdder, LongAdder::doubleValue)
                .tags(tags).tags("level", "errorTellMeTomorrow")
                .description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        // Standard level values (error, warn, info, debug, trace) may also be registered by
        // Micrometer's built-in MetricsTurboFilter. In Micrometer 1.17+ that filter already
        // uses FunctionCounter + LongAdder; in pre-1.17 it uses a plain Counter.
        // removeLegacyCounter() removes any pre-existing Counter before registering the
        // FunctionCounter, so the two types do not conflict.
        // TODO: Legacy - removeLegacyCounter() calls below can be deleted once pre-1.17
        //       Micrometer is no longer supported.
        removeLegacyCounter(registry, "logback.events", tags, "level", "error");
        FunctionCounter.builder("logback.events", errorAdder, LongAdder::doubleValue)
                .tags(tags).tags("level", "error")
                .description("Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)")
                .baseUnit("events")
                .register(registry);

        removeLegacyCounter(registry, "logback.events", tags, "level", "warn");
        FunctionCounter.builder("logback.events", warnAdder, LongAdder::doubleValue)
                .tags(tags).tags("level", "warn")
                .description("Number of warn level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        removeLegacyCounter(registry, "logback.events", tags, "level", "info");
        FunctionCounter.builder("logback.events", infoAdder, LongAdder::doubleValue)
                .tags(tags).tags("level", "info")
                .description("Number of info level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        removeLegacyCounter(registry, "logback.events", tags, "level", "debug");
        FunctionCounter.builder("logback.events", debugAdder, LongAdder::doubleValue)
                .tags(tags).tags("level", "debug")
                .description("Number of debug level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        removeLegacyCounter(registry, "logback.events", tags, "level", "trace");
        FunctionCounter.builder("logback.events", traceAdder, LongAdder::doubleValue)
                .tags(tags).tags("level", "trace")
                .description("Number of trace level events that made it to the logs")
                .baseUnit("events")
                .register(registry);
    }

    /**
     * Removes a plain {@code Counter} meter from the registry if one occupies the given ID,
     * so that a {@link FunctionCounter} can be registered in its place without a type-mismatch error.
     * <p>
     * Pre-1.17 Micrometer's built-in {@code LogbackMetrics} registers plain Counters for
     * standard log levels; 1.17+ uses FunctionCounter. This method bridges the gap during migration.
     * <p>
     * TODO: Legacy - this method can be deleted once pre-1.17 Micrometer is no longer supported.
     */
    private static void removeLegacyCounter(MeterRegistry registry, String name,
            Iterable<Tag> baseTags, String tagKey, String tagValue) {
        Meter existing = registry.find(name).tags(baseTags).tag(tagKey, tagValue).meter();
        if (existing != null && !(existing instanceof FunctionCounter)) {
            registry.remove(existing);
        }
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
                errorAdder.increment();

                List<Marker> markerList = event.getMarkerList();
                if(markerList != null && !markerList.isEmpty()) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(markerList);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorTellMeTomorrowAdder.increment();
                    }
                } else {
                    errorTellMeTomorrowAdder.increment();
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
                traceAdder.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    public void increment(Marker marker, Level level) {
        switch (level.toInt()) {
            case Level.ERROR_INT:
                errorAdder.increment();

                if (marker != null) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(marker);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorTellMeTomorrowAdder.increment();
                    }
                } else {
                    errorTellMeTomorrowAdder.increment();
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
                traceAdder.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(DevOpsLevel severity) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                errorWakeMeUpRightNowAdder.increment();
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                errorInterruptMyDinnerAdder.increment();
                break;
            }
            case ERROR_TELL_ME_TOMORROW: {
                errorTellMeTomorrowAdder.increment();
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
                traceAdder.increment();
                break;
            }
            default: {
                errorTellMeTomorrowAdder.increment();
            }
        }
    }


    public void increment(DevOpsLevel severity, int amount) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                errorWakeMeUpRightNowAdder.add(amount);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                errorInterruptMyDinnerAdder.add(amount);
                break;
            }
            case ERROR_TELL_ME_TOMORROW: {
                errorTellMeTomorrowAdder.add(amount);
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
                traceAdder.add(amount);
                break;
            }
            default: {
                errorTellMeTomorrowAdder.add(amount);
            }
        }
    }
}
