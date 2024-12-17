package no.entur.logging.cloud.spring.ondemand.grpc.lognet.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeProvider;

import java.util.function.Predicate;

public class GrpcContextLoggingScopeFactory implements LoggingScopeFactory<GrpcLoggingScope>, LoggingScopeProvider {

    @Override
    public GrpcLoggingScope openScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate) {
        return new GrpcLoggingScope(queuePredicate, ignorePredicate, logLevelFailurePredicate);
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
