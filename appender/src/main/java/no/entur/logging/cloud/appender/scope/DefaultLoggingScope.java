package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 */
public class DefaultLoggingScope implements LoggingScope {

    protected final Predicate<ILoggingEvent> queuePredicate;
    protected final Predicate<ILoggingEvent> ignorePredicate;
    protected final long timestamp = System.currentTimeMillis();

    protected boolean failure = false;

    protected ConcurrentLinkedQueue<ILoggingEvent> queue = new ConcurrentLinkedQueue<ILoggingEvent>();

    public DefaultLoggingScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate) {
        this.queuePredicate = queuePredicate;
        this.ignorePredicate = ignorePredicate;
    }

    @Override
    public ConcurrentLinkedQueue<ILoggingEvent> getEvents() {
        return queue;
    }

    @Override
    public boolean append(ILoggingEvent eventObject) {
        if(ignorePredicate.test(eventObject)) {
            return true;
        }

        if(!failure) {
            if(queuePredicate.test(eventObject)) {
                // log this event later or not at all
                queue.add(eventObject);
                return true;
            }
        }
        // log this event now
        return false;
    }

    @Override
    public boolean isFailure() {
        return failure;
    }

    @Override
    public void failure() {
        // TODO flush buffer here?
        this.failure = true;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
