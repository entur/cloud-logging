package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.logback.logstash.test.ai.AiTestEvents;
import no.entur.logging.cloud.logback.logstash.test.ai.AiTestThread;

public class CompositeConsoleAsyncAppenderLogging extends LoggingScopeAsyncAppender {

    private CompositeConsoleLoggingEventListener listener;

    public void setListener(CompositeConsoleLoggingEventListener listener) {
        this.listener = listener;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();

        // AI mode: the WARN/ERROR events of a failed test are shown in the summary file
        AiTestEvents.logged(eventObject);

        DefaultCompositeConsoleOutputLoggingEvent event = new DefaultCompositeConsoleOutputLoggingEvent(eventObject, output, AiTestThread.tag());
        super.append(event);
    }

    /**
     * Queues an event for the attached appender without offering it to the listener, so that it is written in order with the other
     * events but is not seen by logging assertions (the test markers of AI mode).
     */
    public void putUnobserved(ILoggingEvent eventObject) {
        super.put(eventObject);
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
