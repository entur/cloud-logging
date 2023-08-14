package no.entur.logging.cloud.appender.scope;

public class NoopLoggingScopeFactory implements LoggingScopeFactory {
    @Override
    public Object openScope() {
        return null;
    }

    @Override
    public LoggingScope getScope() {
        return null;
    }

    @Override
    public void closeScope() {
    }
}
