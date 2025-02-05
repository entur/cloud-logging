package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogLevelLoggingScopeTest {

    private Predicate<ILoggingEvent> infoPredicate = (e) -> e.getLevel().toInt() <= Level.INFO_INT;
    private Predicate<ILoggingEvent> debugPredicate = (e) -> e.getLevel().toInt() <= Level.DEBUG_INT;
    private Predicate<ILoggingEvent> logLevelFailurePredicate = (e) -> e.getLevel().toInt() >= Level.WARN_INT;

    private ILoggingEvent warnLoggingEvent = mock(ILoggingEvent.class);
    private ILoggingEvent infoLoggingEvent = mock(ILoggingEvent.class);
    private ILoggingEvent debugLoggingEvent = mock(ILoggingEvent.class);

    @BeforeEach
    public void init() {
        warnLoggingEvent = mock(ILoggingEvent.class);
        when(warnLoggingEvent.getLevel()).thenReturn(Level.WARN);

        infoLoggingEvent = mock(ILoggingEvent.class);
        when(infoLoggingEvent.getLevel()).thenReturn(Level.INFO);

        debugLoggingEvent = mock(ILoggingEvent.class);
        when(debugLoggingEvent.getLevel()).thenReturn(Level.DEBUG);
    }

    @Test
    public void testIgnoreDebugEagerSuccess() {
        LogLevelLoggingScope eager = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.EAGER);
        eager.append(debugLoggingEvent);
        assertTrue(eager.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreDebugLazySuccess() {
        LogLevelLoggingScope eager = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.LAZY);
        eager.append(debugLoggingEvent);
        assertTrue(eager.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreDebugEagerFailure() {
        LogLevelLoggingScope lazy = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.EAGER);
        lazy.failure();
        lazy.append(debugLoggingEvent);
        assertTrue(lazy.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreDebugLazyFailure() {
        LogLevelLoggingScope lazy = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.LAZY);
        lazy.failure();
        lazy.append(debugLoggingEvent);
        assertTrue(lazy.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreInfoEagerSuccess() {
        LogLevelLoggingScope eager = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.EAGER);
        eager.append(infoLoggingEvent);
        assertTrue(eager.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreInfoLazySuccess() {
        LogLevelLoggingScope eager = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.LAZY);
        eager.append(infoLoggingEvent);
        assertTrue(eager.getEvents().isEmpty());
    }

    @Test
    public void testKeepInfoEagerFailureCachesBefore() {
        LogLevelLoggingScope lazy = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.EAGER);
        assertTrue(lazy.append(infoLoggingEvent)); // cached
        lazy.failure();
        assertFalse(lazy.getEvents().isEmpty());
    }

    @Test
    public void testKeepInfoEagerFailureNoCacheAfter() {
        LogLevelLoggingScope lazy = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.EAGER);
        lazy.failure();
        assertFalse(lazy.append(infoLoggingEvent)); // not cached
        assertTrue(lazy.getEvents().isEmpty());
    }

    @Test
    public void testKeepInfoLazyFailureCachesBefore() {
        LogLevelLoggingScope lazy = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.LAZY);
        lazy.append(infoLoggingEvent);
        lazy.failure();
        assertFalse(lazy.getEvents().isEmpty());
    }

    @Test
    public void testKeepInfoLazyFailureCachesAfter() {
        LogLevelLoggingScope lazy = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.LAZY);
        lazy.failure();
        lazy.append(infoLoggingEvent);
        assertFalse(lazy.getEvents().isEmpty());
    }

    @Test
    public void testLogLevelNoTriggersFailure() {
        LogLevelLoggingScope lazy = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.LAZY);

        lazy.append(debugLoggingEvent);
        lazy.append(infoLoggingEvent);
        assertFalse(lazy.isFailure());
    }

    @Test
    public void testLogLevelTriggersFailure() {
        LogLevelLoggingScope lazy = new LogLevelLoggingScope(infoPredicate, debugPredicate, logLevelFailurePredicate, LoggingScopeFlushMode.LAZY);

        lazy.append(warnLoggingEvent);
        assertTrue(lazy.isFailure());
    }

}
