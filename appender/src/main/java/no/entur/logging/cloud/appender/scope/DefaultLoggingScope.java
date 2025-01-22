package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 */
public class DefaultLoggingScope implements LoggingScope {

    private final Predicate<ILoggingEvent> queuePredicate;
    private final Predicate<ILoggingEvent> ignorePredicate;

    private final Predicate<ILoggingEvent> logLevelFailurePredicate;

    private boolean logLevelFailure = false;

    private ConcurrentLinkedQueue<ILoggingEvent> queue = new ConcurrentLinkedQueue<ILoggingEvent>();

    public DefaultLoggingScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate) {
        this.queuePredicate = queuePredicate;
        this.ignorePredicate = ignorePredicate;
        this.logLevelFailurePredicate = logLevelFailurePredicate;
    }

    public ConcurrentLinkedQueue<ILoggingEvent> getEvents() {
        return queue;
    }

    public boolean append(ILoggingEvent eventObject) {
        if(ignorePredicate.test(eventObject)) {
            return true;
        }

        if(!logLevelFailure && logLevelFailurePredicate.test(eventObject)) {
            logLevelFailure = true;
        }

        if(queuePredicate.test(eventObject)) {
            if(logLevelFailure) {
                // log now, no point in buffering this
                return false;
            }
            queue.add(eventObject);

            return true;
        }
        return false;
    }

    public boolean isLogLevelFailure() {
        return logLevelFailure;
    }
}
