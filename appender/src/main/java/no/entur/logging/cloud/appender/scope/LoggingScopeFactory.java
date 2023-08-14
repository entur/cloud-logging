package no.entur.logging.cloud.appender.scope;

public interface LoggingScopeFactory<T> {

    <T> T openScope();

    LoggingScope getScope();

    void closeScope();

}
