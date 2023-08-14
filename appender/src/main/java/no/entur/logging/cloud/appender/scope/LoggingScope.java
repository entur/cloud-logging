package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 */
public class LoggingScope {

    private final Predicate<ILoggingEvent> filter;

    private ConcurrentLinkedQueue<ILoggingEvent> queue = new ConcurrentLinkedQueue<ILoggingEvent>();

    public LoggingScope(Predicate<ILoggingEvent> filter) {
        this.filter = filter;
    }

    public ConcurrentLinkedQueue<ILoggingEvent> getEvents() {
        return queue;
    }

    public boolean append(ILoggingEvent eventObject) {
        if(filter.test(eventObject)) {
            queue.add(eventObject);

            return true;
        }
        return false;
    }

}
