package no.entur.logging.cloud.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.*;

import java.util.function.Predicate;

public class ThreadLocalLoggingScopeFactory implements LoggingScopeFactory, LoggingScopeControls {

    protected final ThreadLocal<LoggingScope> queues = new ThreadLocal<>();
    protected final LoggingScopeFlushMode flushMode;
    protected final LoggingScopeSink sink;

    public ThreadLocalLoggingScopeFactory(LoggingScopeFlushMode flushMode, LoggingScopeSink sink) {
        this.flushMode = flushMode;
        this.sink = sink;
    }

    @Override
    public LoggingScope openScope(Predicate queuePredicate, Predicate ignorePredicate, Predicate logLevelFailurePredicate) {
        LoggingScope scope;
        if(logLevelFailurePredicate == null) {
            scope = new DefaultLoggingScope(queuePredicate, ignorePredicate, flushMode, sink);
        } else {
            scope = new LogLevelLoggingScope(queuePredicate, ignorePredicate, logLevelFailurePredicate, flushMode, sink);
        }
        queues.set(scope);
        return scope;
    }

    @Override
    public void reopenScope(LoggingScope scope) {
        queues.set(scope);
    }

    @Override
    public LoggingScope getCurrentScope() {
        return queues.get();
    }

    @Override
    public void closeScope(LoggingScope scope) {
        queues.remove();
    }

    public void setCurrentScope(@jakarta.annotation.Nullable LoggingScope scope) {
        if(scope == null) {
            queues.remove();
        } else {
            queues.set(scope);
        }
    }

    public void clearCurrentScope() {
        queues.remove();
    }
}
