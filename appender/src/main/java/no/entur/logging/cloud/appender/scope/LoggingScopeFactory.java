package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.function.Predicate;

public interface LoggingScopeFactory<T extends LoggingScope> {

    T openScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate, Predicate<ILoggingEvent> logLevelFailurePredicate);

    void reconnectScope(LoggingScope scope);

    void closeScope(LoggingScope scope);

}
