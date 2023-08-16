package no.entur.logging.cloud.gcp.spring.grpc.lognet.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.grpc.Context;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;

import java.util.function.Predicate;

public class GrpcContextLoggingScopeFactory implements LoggingScopeFactory<Context> {

    public static final Context.Key<LoggingScope> KEY_CONTEXT = Context.key("LOGGING_SCOPE_CONTEXT");

    @Override
    public Context openScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate) {
        Context context = Context.current();

        LoggingScope loggingScope = new LoggingScope(queuePredicate, ignorePredicate);

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

}
