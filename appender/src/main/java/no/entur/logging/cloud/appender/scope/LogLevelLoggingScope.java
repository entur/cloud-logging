package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 */
public class LogLevelLoggingScope extends DefaultLoggingScope {

    private final Predicate<ILoggingEvent> logLevelFailurePredicate;

    public LogLevelLoggingScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate) {
        super(queuePredicate, ignorePredicate);
        this.logLevelFailurePredicate = logLevelFailurePredicate;
    }

    public boolean append(ILoggingEvent eventObject) {
        if(ignorePredicate.test(eventObject)) {
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
                queue.add(eventObject);
                return true;
            }
        }
        // log this event now
        return false;
    }

}
