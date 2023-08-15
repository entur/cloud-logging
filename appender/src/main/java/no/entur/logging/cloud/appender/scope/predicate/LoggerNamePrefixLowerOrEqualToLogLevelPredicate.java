package no.entur.logging.cloud.appender.scope.predicate;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;

public class LoggerNamePrefixLowerOrEqualToLogLevelPredicate extends LowerOrEqualToLogLevelPredicate {

    private final List<String> prefixes;

    public LoggerNamePrefixLowerOrEqualToLogLevelPredicate(int limit, List<String> prefixes) {
        super(limit);
        this.prefixes = prefixes;
    }

    @Override
    public boolean test(ILoggingEvent iLoggingEvent) {
        if(!super.test(iLoggingEvent)) {
            return false;
        }
        String loggerName = iLoggingEvent.getLoggerName();
        for(String prefix : prefixes) {
            if(loggerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
