package no.entur.logging.cloud.grpc.mdc;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.AsyncAppender;
import org.slf4j.MDC;

import java.util.Map;

public class GrpcMdcContextAsyncAppender extends AsyncAppender {

    @Override
    public void preprocess(ILoggingEvent eventObject) {
        // capture gRPC MDC context, if any
        GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
        if (grpcMdcContext != null) {
            Map<String, String> mdcContext = grpcMdcContext.getContext();
            if (mdcContext.isEmpty()) {
                super.preprocess(eventObject);
            } else {
                MDC.setContextMap(mdcContext);
                try {
                    super.preprocess(eventObject);
                } finally {
                    MDC.clear();
                }
            }
        } else {
            super.preprocess(eventObject);
        }
    }

}
