package no.entur.logging.cloud.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.LoggingScope;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class LoggingScopeThreadUtils {

    protected final LoggingScopeControls controls;

    public LoggingScopeThreadUtils(LoggingScopeControls controls) {
        this.controls = controls;
    }

    public Runnable withLoggingScope(Runnable runnable) {
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

    public <U> Supplier<U> withLoggingScope(Supplier<U> supplier) {
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
