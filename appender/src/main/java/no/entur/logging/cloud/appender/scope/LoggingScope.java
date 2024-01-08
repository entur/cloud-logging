package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Logging scope for temporarily adjusting what gets logged, caching the log skipped-over log statements in the process.
 *
 */
public interface LoggingScope {

    ConcurrentLinkedQueue<ILoggingEvent> getEvents();

    boolean append(ILoggingEvent eventObject);

}
