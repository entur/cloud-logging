package org.entur.logging.grpc.mdc;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;

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
        if(GrpcMdcContext.isWithinContext()) {
            Map<String, String> mdcContext = GrpcMdcContext.get().getContext();

            if(mdcContext.isEmpty()) {
               super.append(eventObject);
            } else {
                Map<String, String> copyOfMdcContext = new HashMap<>(mdcContext);
                super.append(new DelegateILoggingEvent(eventObject, copyOfMdcContext));
            }
        } else {
            super.append(eventObject);
        }
    }



}
