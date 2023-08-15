package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 */
public class LoggingScope {

    private final Predicate<ILoggingEvent> queuePredicate;
    private final Predicate<ILoggingEvent> ignorePredicate;

    private ConcurrentLinkedQueue<ILoggingEvent> queue = new ConcurrentLinkedQueue<ILoggingEvent>();

    public LoggingScope(Predicate<ILoggingEvent> queueFilter, Predicate<ILoggingEvent> ignorePredicate) {
        this.queuePredicate = queueFilter;
        this.ignorePredicate = ignorePredicate;
    }

    public ConcurrentLinkedQueue<ILoggingEvent> getEvents() {
        return queue;
    }

    public boolean append(ILoggingEvent eventObject) {
        if(ignorePredicate.test(eventObject)) {
            return true;
        }
        if(queuePredicate.test(eventObject)) {
            queue.add(eventObject);

            return true;
        }
        return false;
    }

}
