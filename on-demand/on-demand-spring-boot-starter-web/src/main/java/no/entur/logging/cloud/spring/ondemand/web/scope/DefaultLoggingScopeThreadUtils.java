package no.entur.logging.cloud.spring.ondemand.web.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 *
 * Default helper class for propagation of logging scope across multiple threads ++.
 *
 */

public class DefaultLoggingScopeThreadUtils implements LoggingScopeThreadUtils {

    protected final LoggingScopeControls controls;
    protected final LoggingScopeFactory factory;

    protected final Predicate<ILoggingEvent> queuePredicate;
    protected final Predicate<ILoggingEvent> ignorePredicate;
    protected final Predicate<ILoggingEvent> logLevelFailurePredicate;

    public DefaultLoggingScopeThreadUtils(LoggingScopeControls controls, LoggingScopeFactory factory, Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate) {
        this.controls = controls;
        this.factory = factory;
        this.queuePredicate = queuePredicate;
        this.ignorePredicate = ignorePredicate;
        this.logLevelFailurePredicate = logLevelFailurePredicate;
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

    @Override
    public <U> Callable<U> withCallable(Callable<U> callable) {
        LoggingScope currentScope = controls.getCurrentScope();
        if(currentScope == null) {
            return callable;
        }
        return (Callable) () -> {
            controls.setCurrentScope(currentScope);
            try {
                return callable.call();
            } finally {
                controls.clearCurrentScope();
            }
        };
    }

    @Override
    public void withNewScopeWriteManually(Consumer<LoggingScope> consumer) {
        LoggingScope loggingScope = factory.openScope(queuePredicate, ignorePredicate, logLevelFailurePredicate);

        controls.setCurrentScope(loggingScope);
        try {
            consumer.accept(loggingScope);
        } finally {
            controls.clearCurrentScope();
        }
    }

    @Override
    public void withNewScopeWriteAutomatically(Runnable runnable) {
        LoggingScope loggingScope = factory.openScope(queuePredicate, ignorePredicate, logLevelFailurePredicate);

        controls.setCurrentScope(loggingScope);
        try {
            runnable.run();
        } finally {
            loggingScope.write();

            controls.clearCurrentScope();
        }
    }


}
