package no.entur.logging.cloud.azure.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.DefaultLoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeProvider;

import java.util.function.Predicate;

public class ThreadLocalLoggingScopeFactory implements LoggingScopeFactory, LoggingScopeProvider {

    private final ThreadLocal<LoggingScope> queues = new ThreadLocal<>();

    @Override
    public LoggingScope openScope(Predicate queuePredicate, Predicate ignorePredicate, Predicate logLevelFailurePredicate) {
        DefaultLoggingScope scope = new DefaultLoggingScope(queuePredicate, ignorePredicate, logLevelFailurePredicate);
        queues.set(scope);
        return scope;
    }

    @Override
    public LoggingScope getCurrentScope() {
        return queues.get();
    }

    @Override
    public void closeScope(LoggingScope scope) {
        queues.remove();
    }
}
