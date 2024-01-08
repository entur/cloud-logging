package no.entur.logging.cloud.gcp.spring.grpc.lognet.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.grpc.Context;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeProvider;

import java.util.function.Predicate;

public class GrpcContextLoggingScopeFactory implements LoggingScopeFactory<GrpcLoggingScope>, LoggingScopeProvider {

    @Override
    public GrpcLoggingScope openScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate) {
        return new GrpcLoggingScope(queuePredicate, ignorePredicate);
    }

    @Override
    public void closeScope(LoggingScope scope) {
        // do nothing
    }

    @Override
    public LoggingScope getCurrentScope() {
        return GrpcLoggingScopeContextInterceptor.KEY_CONTEXT.get();
    }
}
