package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;

public class CompositeConsoleAsyncAppenderLogging extends LoggingScopeAsyncAppender {

    private CompositeConsoleLoggingEventListener listener;

    public void setListener(CompositeConsoleLoggingEventListener listener) {
        this.listener = listener;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();

        DefaultCompositeConsoleOutputLoggingEvent event = new DefaultCompositeConsoleOutputLoggingEvent(eventObject, output);
        super.append(event);
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
