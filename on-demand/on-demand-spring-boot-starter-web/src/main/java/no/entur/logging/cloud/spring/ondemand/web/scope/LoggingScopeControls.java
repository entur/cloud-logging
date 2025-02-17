package no.entur.logging.cloud.spring.ondemand.web.scope;

import jakarta.annotation.Nullable;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeProvider;

public interface LoggingScopeControls extends LoggingScopeProvider {

    void setCurrentScope(@Nullable LoggingScope scope);

    void clearCurrentScope();

}
