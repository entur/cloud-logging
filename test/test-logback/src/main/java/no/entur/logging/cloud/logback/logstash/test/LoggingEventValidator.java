package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Validates a log statement, throwing if it is invalid. Invoked synchronously, on the calling
 * thread, before the event is handed off to the async appender - so a thrown exception propagates
 * straight back to the code that made the log statement, instead of surfacing later (or being lost)
 * on the appender's background worker thread.
 *
 * @see CompositeConsoleAsyncAppenderLogging#setValidator(LoggingEventValidator)
 */
@FunctionalInterface
public interface LoggingEventValidator {

    /**
     * @param event the log statement to validate
     * @throws RuntimeException (typically {@link IllegalStateException}) if the log statement is invalid
     */
    void validate(ILoggingEvent event);
}
