package no.entur.logging.cloud.appender.scope;

import java.util.function.Predicate;

public class NoopLoggingScopeFactory implements LoggingScopeFactory {
    @Override
    public Object openScope(Predicate queuePredicate, Predicate ignorePredicate) {
        return null;
    }

    @Override
    public LoggingScope getScope() {
        return null;
    }

    @Override
    public void closeScope() {
    }
}
