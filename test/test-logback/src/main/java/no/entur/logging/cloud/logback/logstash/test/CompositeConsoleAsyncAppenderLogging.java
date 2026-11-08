package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;

public class CompositeConsoleAsyncAppenderLogging extends LoggingScopeAsyncAppender {

    private CompositeConsoleLoggingEventListener listener;

    /**
     * Validates every log statement, unless validation has been switched off, see
     * {@link CompositeConsoleOutputControl#disableValidation()}.
     */
    private LoggingEventValidator validator;

    /**
     * Set while the validator is running on the current thread. Should validating an event itself end up
     * logging (e.g. from a marker, getter or supplier which the validator replays), that nested event is
     * still appended as usual, but not validated in turn - which would otherwise recurse without bound.
     */
    private final ThreadLocal<Boolean> validating = new ThreadLocal<>();

    public void setListener(CompositeConsoleLoggingEventListener listener) {
        this.listener = listener;
    }

    public void setValidator(LoggingEventValidator validator) {
        this.validator = validator;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();

        DefaultCompositeConsoleOutputLoggingEvent event = new DefaultCompositeConsoleOutputLoggingEvent(eventObject, output);
        super.append(event);
    }

    /**
     * Appends the event as usual, then runs the {@linkplain #setValidator(LoggingEventValidator) validator}, if any.
     *
     * <p>Validation takes place here, rather than in {@link #append(ILoggingEvent)}, because the base class
     * would otherwise swallow any exception thrown by the validator as an internal status message, instead
     * of letting it propagate back to the log statement's call site as intended. It also means the event has
     * already been written/queued (including any contributed MDC) by the time it is validated, so the
     * offending log line still makes it out, and the validator sees exactly the MDC which will be logged.
     * The validator is applied to every event, regardless of this appender's filters and of whether the
     * event was discarded due to a full queue, unless {@linkplain CompositeConsoleOutputControl#isValidationEnabled()
     * validation has been switched off}. That switch is static, rather than state of this appender, so it is
     * not lost when logback is re-initialized, which replaces this appender.
     *
     * <p>Since the exception propagates through logback's appender loop, appenders attached to the same
     * logger after this one do not receive an event which fails validation.
     */
    @Override
    public void doAppend(ILoggingEvent eventObject) {
        super.doAppend(eventObject);

        LoggingEventValidator validator = this.validator; // defensive copy
        if (validator == null || !isStarted() || !CompositeConsoleOutputControl.isValidationEnabled() || validating.get() != null) {
            return;
        }

        validating.set(Boolean.TRUE);
        try {
            validator.validate(eventObject);
        } finally {
            validating.remove();
        }
    }

    @Override
    public void put(ILoggingEvent eventObject) {
        super.put(eventObject);

        CompositeConsoleLoggingEventListener listener = this.listener; // defensive copy
        if(listener != null) {
            listener.put(eventObject);
        }
    }
}
