package no.entur.logging.cloud.micrometer;

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

import java.util.List;
import java.util.concurrent.atomic.LongAdder;

public class DevOpsMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    protected final LongAdder errorWakeMeUpRightNowCount;
    protected final LongAdder errorInterruptMyDinnerCount;
    protected final LongAdder errorTellMeTomorrowCount;

    protected final LongAdder errorCount; // alias for all errors / backwards compatibility

    protected final LongAdder warnCount;
    protected final LongAdder infoCount;
    protected final LongAdder debugCount;
    protected final LongAdder traceCount;

    public DevOpsMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        errorWakeMeUpRightNowCount = new LongAdder();
        FunctionCounter.builder("logback.events", errorWakeMeUpRightNowCount, LongAdder::doubleValue)
                .tags(tags).tags("level", "errorWakeMeUpRightNow")
                .description("Number of error 'Wake Me Up Right Now' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        errorInterruptMyDinnerCount = new LongAdder();
        FunctionCounter.builder("logback.events", errorInterruptMyDinnerCount, LongAdder::doubleValue)
                .tags(tags).tags("level", "errorInterruptMyDinner")
                .description("Number of error 'Interrupt My Dinner' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        errorTellMeTomorrowCount = new LongAdder();
        FunctionCounter.builder("logback.events", errorTellMeTomorrowCount, LongAdder::doubleValue)
                .tags(tags).tags("level", "errorTellMeTomorrow")
                .description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        errorCount = new LongAdder();
        FunctionCounter.builder("logback.events", errorCount, LongAdder::doubleValue)
                .tags(tags).tags("level", "error")
                .description("Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)")
                .baseUnit("events")
                .register(registry);

        warnCount = new LongAdder();
        FunctionCounter.builder("logback.events", warnCount, LongAdder::doubleValue)
                .tags(tags).tags("level", "warn")
                .description("Number of warn level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        infoCount = new LongAdder();
        FunctionCounter.builder("logback.events", infoCount, LongAdder::doubleValue)
                .tags(tags).tags("level", "info")
                .description("Number of info level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        debugCount = new LongAdder();
        FunctionCounter.builder("logback.events", debugCount, LongAdder::doubleValue)
                .tags(tags).tags("level", "debug")
                .description("Number of debug level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        traceCount = new LongAdder();
        FunctionCounter.builder("logback.events", traceCount, LongAdder::doubleValue)
                .tags(tags).tags("level", "trace")
                .description("Number of trace level events that made it to the logs")
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
                errorCount.increment();

                List<Marker> markerList = event.getMarkerList();
                if(markerList != null && !markerList.isEmpty()) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(markerList);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorTellMeTomorrowCount.increment();
                    }
                } else {
                    errorTellMeTomorrowCount.increment();
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
                traceCount.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    public void increment(Marker marker, Level level) {
        switch (level.toInt()) {
            case Level.ERROR_INT:
                errorCount.increment();

                if (marker != null) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(marker);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorTellMeTomorrowCount.increment();
                    }
                } else {
                    errorTellMeTomorrowCount.increment();
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
                traceCount.increment();
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(DevOpsLevel severity) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                errorWakeMeUpRightNowCount.increment();
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                errorInterruptMyDinnerCount.increment();
                break;
            }
            case ERROR_TELL_ME_TOMORROW: {
                errorTellMeTomorrowCount.increment();
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
                traceCount.increment();
                break;
            }
            default: {
                errorTellMeTomorrowCount.increment();
            }
        }
    }


    public void increment(DevOpsLevel severity, int amount) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                errorWakeMeUpRightNowCount.add(amount);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                errorInterruptMyDinnerCount.add(amount);
                break;
            }
            case ERROR_TELL_ME_TOMORROW: {
                errorTellMeTomorrowCount.add(amount);
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
                traceCount.add(amount);
                break;
            }
            default: {
                errorTellMeTomorrowCount.add(amount);
            }
        }
    }
}
