package no.entur.logging.cloud.grpc.mdc.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.grpc.Context;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;

import java.util.function.Predicate;

public class GrpcContextLoggingScopeFactory implements LoggingScopeFactory<Context> {

    public static final Context.Key<LoggingScope> KEY_CONTEXT = Context.key("LOGGING_SCOPE_CONTEXT");

    protected Predicate<ILoggingEvent> filter = (e) -> false;

    @Override
    public Context openScope() {
        Context context = Context.current();

        LoggingScope loggingScope = new LoggingScope(filter);

        return context.withValue(KEY_CONTEXT, loggingScope);
    }

    @Override
    public LoggingScope getScope() {
        return KEY_CONTEXT.get();
    }

    @Override
    public void closeScope() {
        // do nothing
    }

    public void setFilter(Predicate<ILoggingEvent> filter) {
        this.filter = filter;
    }
}
