package no.entur.logging.cloud.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.LoggingScope;

import java.util.function.Supplier;

/**
 *
 * Default helper class for propagation of logging scope across multiple threads ++.
 *
 */

public class DefaultLoggingScopeThreadUtils implements LoggingScopeThreadUtils {

    protected final LoggingScopeControls controls;

    public DefaultLoggingScopeThreadUtils(LoggingScopeControls controls) {
        this.controls = controls;
    }

    /**
     *
     * Manually trigger failure in the current logging scope, i.e. (usually) get more logging in the current scope.
     *
     */

    public void failure() {
        LoggingScope currentScope = controls.getCurrentScope();
        if(currentScope != null) {
            currentScope.failure();
        }
    }

    public Runnable with(Runnable runnable) {
        LoggingScope currentScope = controls.getCurrentScope();
        if(currentScope == null) {
            return runnable;
        }

        return () -> {
            controls.setCurrentScope(currentScope);
            try {
                runnable.run();
            } finally {
                controls.clearCurrentScope();
            }
        };
    }

    public <U> Supplier<U> with(Supplier<U> supplier) {
        LoggingScope currentScope = controls.getCurrentScope();
        if(currentScope == null) {
            return supplier;
        }
        return (Supplier) () -> {
            controls.setCurrentScope(currentScope);
            try {
                return supplier.get();
            } finally {
                controls.clearCurrentScope();
            }
        };
    }

}
