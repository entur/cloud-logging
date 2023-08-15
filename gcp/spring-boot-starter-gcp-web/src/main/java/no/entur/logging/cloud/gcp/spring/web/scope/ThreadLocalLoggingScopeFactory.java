package no.entur.logging.cloud.gcp.spring.web.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;

import java.util.function.Predicate;

public class ThreadLocalLoggingScopeFactory implements LoggingScopeFactory {

    private final ThreadLocal<LoggingScope> queues = new ThreadLocal<>();

    @Override
    public Object openScope(Predicate queuePredicate, Predicate ignorePredicate) {
        queues.set(new LoggingScope(queuePredicate, ignorePredicate));
        return null;
    }

    @Override
    public LoggingScope getScope() {
        return queues.get();
    }

    @Override
    public void closeScope() {
        queues.remove();
    }
}
