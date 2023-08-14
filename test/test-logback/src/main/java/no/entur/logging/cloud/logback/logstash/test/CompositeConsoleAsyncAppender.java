package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositeConsoleAsyncAppender extends LoggingScopeAsyncAppender {

    @Override
    protected void append(ILoggingEvent eventObject) {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();
        super.append(new DefaultCompositeConsoleOutputLoggingEvent(eventObject, output));
    }

}
