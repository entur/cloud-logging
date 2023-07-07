package no.entur.logging.cloud.gcp.spring.grpc.test;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.DefaultCompositeConsoleOutputLoggingEvent;

import java.util.HashMap;
import java.util.Map;

public class GrpcMdcContextCompositeConsoleAsyncAppender extends AsyncAppender {

    @Override
    protected void append(ILoggingEvent eventObject) {

        if (GrpcMdcContext.isWithinContext()) {
            Map<String, String> mdcContext = GrpcMdcContext.get().getContext();
            if (mdcContext.isEmpty()) {
                super.append(new DefaultCompositeConsoleOutputLoggingEvent(eventObject, CompositeConsoleOutputControl.getOutput()));
            } else {
                // TODO this currently does not work with testing, since it is not possible
                // to set the MDC map twice in the original ILoggingEvent
                Map<String, String> copyOfMdcContext = new HashMap<>(mdcContext);
                super.append(new MdcCompositeConsoleOutputDelegateILoggingEvent(eventObject, CompositeConsoleOutputControl.getOutput(), copyOfMdcContext));
            }
        } else {
            super.append(new DefaultCompositeConsoleOutputLoggingEvent(eventObject, CompositeConsoleOutputControl.getOutput()));
        }
    }
}
