package no.entur.logging.cloud.appender.scope;

import java.util.function.Predicate;

public class NoopLoggingScopeFactory implements LoggingScopeFactory, LoggingScopeProvider {
    @Override
    public LoggingScope openScope(Predicate queuePredicate, Predicate ignorePredicate) {
        return null;
    }

    @Override
    public LoggingScope getCurrentScope() {
        return null;
    }

    @Override
    public void closeScope(LoggingScope scope) {
    }
}
