package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.function.Predicate;

public interface LoggingScopeFactory<T> {

    <T> T openScope(Predicate<ILoggingEvent> queuePredicate, Predicate<ILoggingEvent> ignorePredicate);

    LoggingScope getScope();

    void closeScope();

}
