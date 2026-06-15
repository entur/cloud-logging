package no.entur.logging.cloud.micrometer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import no.entur.logging.cloud.micrometer.counter.EventCounter;
import no.entur.logging.cloud.micrometer.counter.EventCounterFactory;
import org.slf4j.Marker;

import java.util.List;

public class DevOpsMetricsTurboFilter extends TurboFilter implements LoggingEventMetrics {

    private static final EventCounterFactory COUNTER_FACTORY = EventCounterFactory.forCurrentSpringBootVersion();

    protected final EventCounter errorWakeMeUpRightNowCount;
    protected final EventCounter errorInterruptMyDinnerCount;
    protected final EventCounter errorTellMeTomorrowCount;

    protected final EventCounter errorCount; // alias for all errors / backwards compatibility

    protected final EventCounter warnCount;
    protected final EventCounter infoCount;
    protected final EventCounter debugCount;
    protected final EventCounter traceCount;

    public DevOpsMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
        errorWakeMeUpRightNowCount = COUNTER_FACTORY.register("logback.events", registry, tags,
                "level", "errorWakeMeUpRightNow",
                "Number of error 'Wake Me Up Right Now' level events that made it to the logs");

        errorInterruptMyDinnerCount = COUNTER_FACTORY.register("logback.events", registry, tags,
                "level", "errorInterruptMyDinner",
                "Number of error 'Interrupt My Dinner' level events that made it to the logs");

        errorTellMeTomorrowCount = COUNTER_FACTORY.register("logback.events", registry, tags,
                "level", "errorTellMeTomorrow",
                "Number of error 'Tell Me Tomorrow' level events that made it to the logs");

        errorCount = COUNTER_FACTORY.register("logback.events", registry, tags,
                "level", "error",
                "Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)");

        warnCount = COUNTER_FACTORY.register("logback.events", registry, tags,
                "level", "warn",
                "Number of warn level events that made it to the logs");

        infoCount = COUNTER_FACTORY.register("logback.events", registry, tags,
                "level", "info",
                "Number of info level events that made it to the logs");

        debugCount = COUNTER_FACTORY.register("logback.events", registry, tags,
                "level", "debug",
                "Number of debug level events that made it to the logs");

        traceCount = COUNTER_FACTORY.register("logback.events", registry, tags,
                "level", "trace",
                "Number of trace level events that made it to the logs");
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
                errorCount.accept(1L);

                List<Marker> markerList = event.getMarkerList();
                if(markerList != null && !markerList.isEmpty()) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(markerList);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorTellMeTomorrowCount.accept(1L);
                    }
                } else {
                    errorTellMeTomorrowCount.accept(1L);
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
                traceCount.accept(1L);
                break;
            default: {
                // do nothing
            }
        }
    }

    public void increment(Marker marker, Level level) {
        switch (level.toInt()) {
            case Level.ERROR_INT:
                errorCount.accept(1L);

                if (marker != null) {
                    DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(marker);
                    if (severity != null) {
                        increment(severity);
                    } else {
                        errorTellMeTomorrowCount.accept(1L);
                    }
                } else {
                    errorTellMeTomorrowCount.accept(1L);
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
                traceCount.accept(1L);
                break;
            default: {
                // do nothing
            }
        }
    }

    protected void increment(DevOpsLevel severity) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                errorWakeMeUpRightNowCount.accept(1L);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                errorInterruptMyDinnerCount.accept(1L);
                break;
            }
            case ERROR_TELL_ME_TOMORROW: {
                errorTellMeTomorrowCount.accept(1L);
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
                traceCount.accept(1L);
                break;
            }
            default: {
                errorTellMeTomorrowCount.accept(1L);
            }
        }
    }

    public void increment(DevOpsLevel severity, int amount) {
        switch (severity) {
            case ERROR_WAKE_ME_UP_RIGHT_NOW: {
                errorWakeMeUpRightNowCount.accept(amount);
                break;
            }
            case ERROR_INTERRUPT_MY_DINNER: {
                errorInterruptMyDinnerCount.accept(amount);
                break;
            }
            case ERROR_TELL_ME_TOMORROW: {
                errorTellMeTomorrowCount.accept(amount);
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
                traceCount.accept(amount);
                break;
            }
            default: {
                errorTellMeTomorrowCount.accept(amount);
            }
        }
    }
}
