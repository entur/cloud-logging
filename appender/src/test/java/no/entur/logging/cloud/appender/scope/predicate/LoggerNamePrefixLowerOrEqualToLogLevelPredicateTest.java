package no.entur.logging.cloud.appender.scope.predicate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoggerNamePrefixLowerOrEqualToLogLevelPredicateTest {

    @Test
    public void testMatchingPrefixAndLevelWithinLimitReturnsTrue() {
        LoggerNamePrefixLowerOrEqualToLogLevelPredicate predicate =
                new LoggerNamePrefixLowerOrEqualToLogLevelPredicate(Level.INFO_INT, List.of("com.example"));
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getLoggerName()).thenReturn("com.example.MyClass");
        assertThat(predicate.test(event)).isTrue();
    }

    @Test
    public void testNonMatchingPrefixReturnsFalse() {
        LoggerNamePrefixLowerOrEqualToLogLevelPredicate predicate =
                new LoggerNamePrefixLowerOrEqualToLogLevelPredicate(Level.INFO_INT, List.of("com.example"));
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getLoggerName()).thenReturn("org.other.MyClass");
        assertThat(predicate.test(event)).isFalse();
    }

    @Test
    public void testMatchingPrefixButLevelAboveLimitReturnsFalse() {
        LoggerNamePrefixLowerOrEqualToLogLevelPredicate predicate =
                new LoggerNamePrefixLowerOrEqualToLogLevelPredicate(Level.INFO_INT, List.of("com.example"));
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.WARN);
        when(event.getLoggerName()).thenReturn("com.example.MyClass");
        assertThat(predicate.test(event)).isFalse();
    }
}
