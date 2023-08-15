package no.entur.logging.cloud.appender.scope.predicate;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.function.Predicate;

public class HigherOrEqualToLogLevelPredicate implements Predicate<ILoggingEvent> {

    protected final int limit;

    public HigherOrEqualToLogLevelPredicate(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean test(ILoggingEvent iLoggingEvent) {
        return iLoggingEvent.getLevel().toInt() >= limit;
    }
}
