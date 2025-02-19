package no.entur.logging.cloud.spring.ondemand.web.scope;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 *
 * Placeholder helper class so that autowiring and so on works as expected
 *
 */

public class NoopLoggingScopeThreadUtils implements LoggingScopeThreadUtils {

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
}
