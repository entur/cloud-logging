package no.entur.logging.cloud.spring.ondemand.grpc.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.grpc.Context;
import no.entur.logging.cloud.appender.scope.*;

import java.util.function.Predicate;

public class GrpcContextLoggingScopeFactory implements LoggingScopeFactory<LoggingScope>, LoggingScopeProvider {

    protected final LoggingScopeFlushMode flushMode;
    protected final LoggingScopeSink sink;

    public GrpcContextLoggingScopeFactory(LoggingScopeFlushMode flushMode, LoggingScopeSink sink) {
        this.flushMode = flushMode;
        this.sink = sink;

    }

    @Override
    public LoggingScope openScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate) {
        LoggingScope scope;
        if(logLevelFailurePredicate == null) {
            scope = new DefaultLoggingScope(queuePredicate, ignorePredicate, flushMode, sink);
        } else {
            scope = new LogLevelLoggingScope(queuePredicate, ignorePredicate, logLevelFailurePredicate, flushMode, sink);
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
