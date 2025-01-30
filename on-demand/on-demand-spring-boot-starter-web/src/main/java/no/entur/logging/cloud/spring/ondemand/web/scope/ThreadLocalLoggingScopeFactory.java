package no.entur.logging.cloud.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.DefaultLoggingScope;
import no.entur.logging.cloud.appender.scope.LogLevelLoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;

import java.util.function.Predicate;

public class ThreadLocalLoggingScopeFactory implements LoggingScopeFactory, LoggingScopeControls {

    private final ThreadLocal<LoggingScope> queues = new ThreadLocal<>();

    @Override
    public LoggingScope openScope(Predicate queuePredicate, Predicate ignorePredicate, Predicate logLevelFailurePredicate) {
        LoggingScope scope;
        if(logLevelFailurePredicate == null) {
            scope = new DefaultLoggingScope(queuePredicate, ignorePredicate);
        } else {
            scope = new LogLevelLoggingScope(queuePredicate, ignorePredicate, logLevelFailurePredicate);
        }
        queues.set(scope);
        return scope;
    }

    @Override
    public void reconnectScope(LoggingScope scope) {
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

    public void setCurrentScope(LoggingScope scope) {
        queues.set(scope);
    }

    public void clearCurrentScope() {
        queues.remove();
    }
}
