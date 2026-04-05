package no.entur.logging.cloud.appender.scope.predicate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LowerOrEqualToLogLevelPredicateTest {

    @Test
    public void testInfoEqualsInfoPasses() {
        LowerOrEqualToLogLevelPredicate predicate = new LowerOrEqualToLogLevelPredicate(Level.INFO_INT);
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.INFO);
        assertThat(predicate.test(event)).isTrue();
    }

    @Test
    public void testDebugBelowInfoPasses() {
        LowerOrEqualToLogLevelPredicate predicate = new LowerOrEqualToLogLevelPredicate(Level.INFO_INT);
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.DEBUG);
        assertThat(predicate.test(event)).isTrue();
    }

    @Test
    public void testWarnAboveInfoFails() {
        LowerOrEqualToLogLevelPredicate predicate = new LowerOrEqualToLogLevelPredicate(Level.INFO_INT);
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.WARN);
        assertThat(predicate.test(event)).isFalse();
    }
}
