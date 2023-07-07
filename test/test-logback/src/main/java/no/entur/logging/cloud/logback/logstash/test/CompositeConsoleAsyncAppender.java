package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositeConsoleAsyncAppender extends AsyncAppender {

    @Override
    protected void append(ILoggingEvent eventObject) {
        super.append(new DefaultCompositeConsoleOutputLoggingEvent(eventObject, CompositeConsoleOutputControl.getOutput()));
    }

}
