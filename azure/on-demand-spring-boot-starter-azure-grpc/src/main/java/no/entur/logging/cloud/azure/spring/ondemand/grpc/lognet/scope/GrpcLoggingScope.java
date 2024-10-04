package no.entur.logging.cloud.azure.spring.ondemand.grpc.lognet.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.DefaultLoggingScope;

import java.util.function.Predicate;

public class GrpcLoggingScope extends DefaultLoggingScope {

    public GrpcLoggingScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate) {
        super(queuePredicate, ignorePredicate, logLevelFailurePredicate);
    }
}
