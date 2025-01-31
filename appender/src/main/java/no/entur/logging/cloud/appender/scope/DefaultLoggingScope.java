package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 */
public class DefaultLoggingScope implements LoggingScope {

    // note: not ignored and not queued means it should be printed
    protected final Predicate<ILoggingEvent> queuePredicate;
    protected final Predicate<ILoggingEvent> ignorePredicate;

    protected final long timestamp = System.currentTimeMillis();

    protected boolean failure = false;

    protected final LoggingScopeFlushMode flushMode;

    protected ConcurrentLinkedQueue<ILoggingEvent> events = new ConcurrentLinkedQueue<ILoggingEvent>();

    public DefaultLoggingScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, LoggingScopeFlushMode flushMode) {
        this.queuePredicate = queuePredicate;
        this.ignorePredicate = ignorePredicate;
        this.flushMode = flushMode;
    }

    @Override
    public Queue<ILoggingEvent> getEvents() {
        if(flushMode == LoggingScopeFlushMode.LAZY && !failure) {
            // filter unwanted events
            events.removeIf(queuePredicate);
        }

        return events;
    }

    @Override
    public boolean append(ILoggingEvent eventObject) {
        if(ignorePredicate.test(eventObject)) {
            return true;
        }

        if(flushMode == LoggingScopeFlushMode.LAZY) {
            // queue for later processing
            events.add(eventObject);
            return true;
        }

        if(!failure) {
            if(queuePredicate.test(eventObject)) {
                // log this event later or not at all
                events.add(eventObject);
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
        this.failure = true;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
