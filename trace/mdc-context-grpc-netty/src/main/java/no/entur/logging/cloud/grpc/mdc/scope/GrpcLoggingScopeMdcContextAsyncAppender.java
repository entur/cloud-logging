package no.entur.logging.cloud.grpc.mdc.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import org.slf4j.MDC;

import java.util.Map;

public class GrpcLoggingScopeMdcContextAsyncAppender extends LoggingScopeAsyncAppender {

    @Override
    public void preprocess(ILoggingEvent eventObject) {
        // capture gRPC MDC context, if any
        if (GrpcMdcContext.isWithinContext()) {
            Map<String, String> mdcContext = GrpcMdcContext.get().getContext();
            if (mdcContext.isEmpty()) {
                super.preprocess(eventObject);
            } else {
                MDC.setContextMap(mdcContext);
                // TODO this currently does not work with testing, since it is not possible
                // to set the MDC map twice in the original ILoggingEvent
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
