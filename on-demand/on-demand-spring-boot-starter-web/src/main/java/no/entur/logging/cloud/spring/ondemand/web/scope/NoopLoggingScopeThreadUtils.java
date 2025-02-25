package no.entur.logging.cloud.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.LoggingScope;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * Placeholder helper class so that autowiring and so on works as expected
 *
 */

public class NoopLoggingScopeThreadUtils implements LoggingScopeThreadUtils {

    private static final NoopLoggingScope NOOP_LOGGING_SCOPE = new NoopLoggingScope();

    private static final Closeable NOOP_CLOSABLE = new Closeable() {
        @Override
        public void close() throws IOException {
            // do nothing
        }
    };

    @Override
    public void failure() {
        // do nothing
    }

    @Override
    public Runnable with(Runnable runnable) {
        return runnable;
    }

    @Override
    public <U> Supplier<U> with(Supplier<U> supplier) {
        return supplier;
    }

    @Override
    public <U> Callable<U> withCallable(Callable<U> callable) {
        return callable;
    }

    @Override
    public void withNewScopeWriteManually(Consumer<LoggingScope> consumer) {
        consumer.accept(NOOP_LOGGING_SCOPE);
    }

    @Override
    public void withNewScopeWriteAutomatically(Runnable runnable) {
        runnable.run();
    }

}
