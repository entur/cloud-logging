package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 *  Logging scopes are thread safe.
 */
public interface LoggingScope {

    Queue<ILoggingEvent> getEvents();

    boolean append(ILoggingEvent eventObject);

    boolean isFailure();

    /**
     *
     * Manually trigger failure condition for this scope.
     *
     */

    void failure();

    /**
     * Get the logging scope creation timestamp.
     *
     * @return
     */

    long getTimestamp();

    /**
     *
     * Flush the current events. This will result in the relevant log statements being written.
     *
     */

    void write();
}
