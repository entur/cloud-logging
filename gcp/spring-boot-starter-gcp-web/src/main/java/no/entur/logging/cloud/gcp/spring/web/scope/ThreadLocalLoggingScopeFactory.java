package no.entur.logging.cloud.gcp.spring.web.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;

import java.util.function.Predicate;

public class ThreadLocalLoggingScopeFactory implements LoggingScopeFactory {

    private final ThreadLocal<LoggingScope> queues = new ThreadLocal<>();

    protected Predicate<ILoggingEvent> filter = (e) -> false;

    public void setFilter(Predicate<ILoggingEvent> filter) {
        this.filter = filter;
    }

    @Override
    public Object openScope() {
        queues.set(new LoggingScope(filter));
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
