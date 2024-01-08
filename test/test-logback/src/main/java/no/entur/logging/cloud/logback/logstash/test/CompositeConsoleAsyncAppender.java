package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.ScopeAsyncAppender;

public class CompositeConsoleAsyncAppender extends ScopeAsyncAppender {

    @Override
    protected void append(ILoggingEvent eventObject) {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();
        super.append(new DefaultCompositeConsoleOutputLoggingEvent(eventObject, output));
    }

}
