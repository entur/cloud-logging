package no.entur.logging.cloud.grpc.mdc;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

import java.util.HashMap;
import java.util.Map;

public class GrpcMdcContextAsyncAppender extends AsyncAppender {

    @Override
    protected void preprocess(ILoggingEvent eventObject) {
        super.preprocess(eventObject);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {

        // capture gRPC MDC context, if any
        if (GrpcMdcContext.isWithinContext()) {
            Map<String, String> mdcContext = GrpcMdcContext.get().getContext();
            if (mdcContext.isEmpty()) {
                super.append(eventObject);
            } else {
                // TODO this currently does not work with testing, since it is not possible
                // to set the MDC map twice in the original ILoggingEvent
                Map<String, String> copyOfMdcContext = new HashMap<>(mdcContext);
                super.append(new DelegateILoggingEvent(eventObject, copyOfMdcContext));
            }
        } else {
            super.append(eventObject);
        }
    }

}
