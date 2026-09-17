package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.ReentryGuard;
import ch.qos.logback.core.util.SimpleTimeBasedGuard;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;

public class CompositeConsoleAsyncAppenderLogging extends LoggingScopeAsyncAppender {

    private CompositeConsoleLoggingEventListener listener;

    private LoggingEventValidator validator;
    private ReentryGuard validationReentryGuard = buildReentryGuard();
    private final SimpleTimeBasedGuard exceptionGuard = new SimpleTimeBasedGuard();

    public void setListener(CompositeConsoleLoggingEventListener listener) {
        this.listener = listener;
    }

    public void setValidator(LoggingEventValidator validator) {
        this.validator = validator;
    }

    @Override
    public void start() {
        this.validationReentryGuard = buildReentryGuard();
        super.start();
    }

    @Override
    public void doAppend(ILoggingEvent eventObject) {
        if (!isStarted()) {
            super.doAppend(eventObject);
            return;
        }

        ReentryGuard validationReentryGuard = this.validationReentryGuard;
        if (validationReentryGuard.isLocked()) {
            return;
        }
        validationReentryGuard.lock();
        try {
            if (getFilterChainDecision(eventObject) == FilterReply.DENY) {
                return;
            }

            ILoggingEvent event = prepareEvent(eventObject);
            if (event == null) {
                return;
            }

            validate(event);
            try {
                writePreparedEvent(event);
            } catch (Exception e) {
                if (exceptionGuard.allow()) {
                    addError("Appender [" + name + "] failed to append.", e);
                }
            }
        } finally {
            validationReentryGuard.unlock();
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        ILoggingEvent event = prepareEvent(eventObject);
        if (event == null) {
            return;
        }
        validate(event);
        writePreparedEvent(event);
    }

    private ILoggingEvent prepareEvent(ILoggingEvent eventObject) {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();

        DefaultCompositeConsoleOutputLoggingEvent event = new DefaultCompositeConsoleOutputLoggingEvent(eventObject, output);
        if (isQueueBelowDiscardingThreshold() && isDiscardable(event)) {
            return null;
        }
        preprocess(event);
        return event;
    }

    private void validate(ILoggingEvent event) {
        LoggingEventValidator validator = this.validator; // defensive copy
        if (validator != null) {
            validator.validate(event);
        }
    }

    private void writePreparedEvent(ILoggingEvent event) {
        LoggingScope scope = getCurrentScope();
        if(scope == null || !scope.append(event)) {
            super.write(event);
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
