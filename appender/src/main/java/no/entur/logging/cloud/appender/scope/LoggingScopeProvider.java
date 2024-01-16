package no.entur.logging.cloud.appender.scope;

@FunctionalInterface
public interface LoggingScopeProvider {

    LoggingScope getCurrentScope();
}
