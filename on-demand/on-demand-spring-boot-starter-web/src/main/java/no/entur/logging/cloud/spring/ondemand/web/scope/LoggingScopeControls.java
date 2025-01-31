package no.entur.logging.cloud.spring.ondemand.web.scope;

import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeProvider;

public interface LoggingScopeControls extends LoggingScopeProvider {

    void setCurrentScope(LoggingScope scope);

    void clearCurrentScope();

}
