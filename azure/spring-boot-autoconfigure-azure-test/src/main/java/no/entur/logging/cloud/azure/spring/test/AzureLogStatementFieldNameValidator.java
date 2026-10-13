package no.entur.logging.cloud.azure.spring.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.LoggingEventValidator;

/**
 * Adapts {@link LogStatementFieldNameValidator} to {@link LoggingEventValidator}, so it can be wired
 * onto {@code CompositeConsoleAsyncAppenderLogging} (see {@code logback-test.xml}) and run
 * synchronously, on the calling thread, for every log statement - causing a thrown
 * {@link IllegalStateException} to blow up right where the offending log statement was made, rather
 * than later on the appender's background worker thread.
 *
 * <p>The validation can be switched off with the {@value #ENABLED_PROPERTY} system property, which is
 * what {@code logback-test.xml} binds {@link #setEnabled(boolean)} to.
 */
public class AzureLogStatementFieldNameValidator implements LoggingEventValidator {

    /**
     * System property controlling whether log statements are validated; defaults to {@code true}.
     */
    public static final String ENABLED_PROPERTY = "entur.logging.test.field-name-validation.enabled";

    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void validate(ILoggingEvent event) {
        if (enabled) {
            LogStatementFieldNameValidator.validate(event);
        }
    }
}
