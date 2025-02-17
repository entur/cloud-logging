package no.entur.logging.cloud.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.LoggingScope;

public class NoopLoggingScopeControls implements LoggingScopeControls {
    @Override
    public void setCurrentScope(LoggingScope scope) {
        // do nothing
    }

    @Override
    public void clearCurrentScope() {
        // do nothing
    }

    @Override
    public LoggingScope getCurrentScope() {
        // do nothing
        return null;
    }
}
