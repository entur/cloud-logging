package no.entur.logging.cloud.gcp.spring.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.LoggingEventValidator;

/**
 * Adapts {@link LogStatementFieldNameValidator} to {@link LoggingEventValidator}, so it can be wired
 * onto {@code CompositeConsoleAsyncAppenderLogging} (see {@code logback-test.xml}) and run
 * synchronously, on the calling thread, for every log statement - causing a thrown
 * {@link IllegalStateException} to blow up right where the offending log statement was made, rather
 * than later on the appender's background worker thread.
 */
public class GcpLogStatementFieldNameValidator implements LoggingEventValidator {

    @Override
    public void validate(ILoggingEvent event) {
        LogStatementFieldNameValidator.validate(event);
    }
}
