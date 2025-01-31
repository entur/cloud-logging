package no.entur.logging.cloud.spring.ondemand.grpc.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.grpc.Context;
import no.entur.logging.cloud.appender.scope.*;

import java.util.function.Predicate;

public class GrpcContextLoggingScopeFactory implements LoggingScopeFactory<LoggingScope>, LoggingScopeProvider {

    protected final LoggingScopeFlushMode flushMode;

    public GrpcContextLoggingScopeFactory(LoggingScopeFlushMode flushMode) {
        this.flushMode = flushMode;

    }

    @Override
    public LoggingScope openScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate) {
        LoggingScope scope;
        if(logLevelFailurePredicate == null) {
            scope = new DefaultLoggingScope(queuePredicate, ignorePredicate, flushMode);
        } else {
            scope = new LogLevelLoggingScope(queuePredicate, ignorePredicate, logLevelFailurePredicate, flushMode);
        }

        return scope;
    }

    @Override
    public void reopenScope(LoggingScope scope) {
        // do nothing
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
