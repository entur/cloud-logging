package no.entur.logging.cloud.spring.ondemand.grpc.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.*;

import java.util.function.Predicate;

public class GrpcContextLoggingScopeFactory implements LoggingScopeFactory<LoggingScope>, LoggingScopeProvider {

    @Override
    public LoggingScope openScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate) {
        LoggingScope scope;
        if(logLevelFailurePredicate == null) {
            scope = new DefaultLoggingScope(queuePredicate, ignorePredicate);
        } else {
            scope = new LogLevelLoggingScope(queuePredicate, ignorePredicate, logLevelFailurePredicate);
        }
        return scope;
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
