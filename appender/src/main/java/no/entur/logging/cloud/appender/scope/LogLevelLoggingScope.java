package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.function.Predicate;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 */
public class LogLevelLoggingScope extends DefaultLoggingScope {

    private final Predicate<ILoggingEvent> logLevelFailurePredicate;

    public LogLevelLoggingScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate, LoggingScopeFlushMode flushMode) {
        super(queuePredicate, ignorePredicate, flushMode);
        this.logLevelFailurePredicate = logLevelFailurePredicate;
    }

    public boolean append(ILoggingEvent eventObject) {
        if(ignorePredicate.test(eventObject)) {
            return true;
        }

        if(flushMode == LoggingScopeFlushMode.LAZY) {
            if(!failure && logLevelFailurePredicate.test(eventObject)) {
                failure();
            }

            // queue for later processing
            events.add(eventObject);
            return true;
        }

        if(!failure) {
            if(logLevelFailurePredicate.test(eventObject)) {
                failure();

                // log this event now
                return false;
            }
            if(queuePredicate.test(eventObject)) {
                // log this event later or not at all
                events.add(eventObject);
                return true;
            }
        }
        // log this event now
        return false;
    }

}
