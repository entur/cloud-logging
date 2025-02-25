package no.entur.logging.cloud.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.LoggingScope;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * Helper class for propagation of logging scope across multiple threads ++.
 *
 */

public interface LoggingScopeThreadUtils {

    /**
     *
     * Manually trigger failure in the current logging scope, i.e. (usually) get more logging in the current scope.
     *
     */

    void failure();

    /**
     * Create wrapper runnable which forwards the current scope
     *
     * @param runnable job
     * @return wrapped job
     */

    Runnable with(Runnable runnable);

    /**
     *
     * Create wrapper runnable which forwards the current scope
     *
     * @param supplier job
     * @return wrapped job
     * @param <U> job output
     */

    <U> Supplier<U> with(Supplier<U> supplier);

    /**
     *
     * Create wrapper callable which forwards the current scope
     *
     * @param callable job
     * @return wrapped job
     * @param <U> job output
     */

    <U> Callable<U> withCallable(Callable<U> callable);

    /**
     *
     * Create new scope, i.e. for running background jobs and so on
     *
     * @param consumer job to wrap. The logging scope is not flushed when the method finishes; caller must flush by calling {@linkplain LoggingScope#write()}.
     */

    void wrapInNewScope(Consumer<LoggingScope> consumer);

    /**
     *
     * Create new scope, i.e. for running background jobs and so on
     *
     * @param runnable job to wrap. The logging scope is flushed when the method finishes.
     */


    void wrapInNewScope(Runnable runnable);
}
