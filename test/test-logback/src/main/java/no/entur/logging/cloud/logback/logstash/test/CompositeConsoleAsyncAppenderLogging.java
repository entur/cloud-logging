package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.ReentryGuard;
import ch.qos.logback.core.util.SimpleTimeBasedGuard;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;

public class CompositeConsoleAsyncAppenderLogging extends LoggingScopeAsyncAppender {

    private CompositeConsoleLoggingEventListener listener;

    private LoggingEventValidator validator;
    private ReentryGuard reentryGuard = buildReentryGuard();
    private final SimpleTimeBasedGuard exceptionGuard = new SimpleTimeBasedGuard();

    public void setListener(CompositeConsoleLoggingEventListener listener) {
        this.listener = listener;
    }

    public void setValidator(LoggingEventValidator validator) {
        this.validator = validator;
    }

    @Override
    public void start() {
        this.reentryGuard = buildReentryGuard();
        super.start();
    }

    /**
     * Overridden (bypassing {@code append()} entirely, which is never reached here as a result) because
     * {@link ch.qos.logback.core.AppenderBase#doAppend} would otherwise swallow any exception thrown by
     * the validator as an internal status message, instead of letting it propagate back to the log
     * statement's call site as intended. The validator only runs after the event has already been
     * written/queued (via the parent's usual {@link #append(ILoggingEvent)} logic), so the offending log
     * line still makes it out even when it also fails validation. A manual reentry guard mirrors
     * {@code AppenderBase}'s own protection against (unbounded) recursive {@code doAppend} calls, e.g.
     * should writing or validating an event itself end up logging.
     */
    @Override
    public void doAppend(ILoggingEvent eventObject) {
        if (!isStarted()) {
            super.doAppend(eventObject);
            return;
        }

        ReentryGuard reentryGuard = this.reentryGuard; // defensive copy
        if (reentryGuard.isLocked()) {
            return;
        }
        reentryGuard.lock();
        try {
            if (getFilterChainDecision(eventObject) == FilterReply.DENY) {
                return;
            }

            CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();
            ILoggingEvent event = new DefaultCompositeConsoleOutputLoggingEvent(eventObject, output);

            try {
                super.append(event);
            } catch (Exception e) {
                if (exceptionGuard.allow()) {
                    addError("Appender [" + name + "] failed to append.", e);
                }
            }

            LoggingEventValidator validator = this.validator; // defensive copy
            if (validator != null) {
                validator.validate(event);
            }
        } finally {
            reentryGuard.unlock();
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

