package no.entur.logging.cloud.gcp.spring.grpc.lognet.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.DefaultLoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.MdcLoggingScope;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;

import java.util.Map;
import java.util.function.Predicate;

public class GrpcLoggingScope extends DefaultLoggingScope implements MdcLoggingScope {

    public GrpcLoggingScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate) {
        super(queuePredicate, ignorePredicate);
    }

    @Override
    public Map<String, String> getMdcContext() {
        GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
        if(grpcMdcContext != null) {
            return grpcMdcContext.getContext();
        }
        return null;
    }
}
