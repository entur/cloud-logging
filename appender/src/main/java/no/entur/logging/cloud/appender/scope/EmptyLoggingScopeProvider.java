package no.entur.logging.cloud.appender.scope;

public class EmptyLoggingScopeProvider implements LoggingScopeProvider{
    @Override
    public LoggingScope getCurrentScope() {
        return null;
    }
}
