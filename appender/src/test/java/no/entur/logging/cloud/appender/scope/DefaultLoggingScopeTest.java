package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class DefaultLoggingScopeTest {

    private Predicate<ILoggingEvent> infoPredicate = (e) -> e.getLevel().toInt() <= Level.INFO_INT;
    private Predicate<ILoggingEvent> debugPredicate = (e) -> e.getLevel().toInt() <= Level.DEBUG_INT;

    private ILoggingEvent infoLoggingEvent = mock(ILoggingEvent.class);
    private ILoggingEvent debugLoggingEvent = mock(ILoggingEvent.class);
    private LoggingScopeSink sink = mock(LoggingScopeSink.class);

    @BeforeEach
    public void init() {
        infoLoggingEvent = mock(ILoggingEvent.class);
        when(infoLoggingEvent.getLevel()).thenReturn(Level.INFO);

        debugLoggingEvent = mock(ILoggingEvent.class);
        when(debugLoggingEvent.getLevel()).thenReturn(Level.DEBUG);
    }

    @Test
    public void testIgnoreDebugEagerSuccess() {
        DefaultLoggingScope eager = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.EAGER, sink);
        eager.append(debugLoggingEvent);
        assertTrue(eager.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreDebugLazySuccess() {
        DefaultLoggingScope eager = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.LAZY, sink);
        eager.append(debugLoggingEvent);
        assertTrue(eager.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreDebugEagerFailure() {
        DefaultLoggingScope lazy = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.EAGER, sink);
        lazy.failure();
        lazy.append(debugLoggingEvent);
        assertTrue(lazy.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreDebugLazyFailure() {
        DefaultLoggingScope lazy = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.LAZY, sink);
        lazy.failure();
        lazy.append(debugLoggingEvent);
        assertTrue(lazy.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreInfoEagerSuccess() {
        DefaultLoggingScope eager = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.EAGER, sink);
        eager.append(infoLoggingEvent);
        assertTrue(eager.getEvents().isEmpty());
    }

    @Test
    public void testIgnoreInfoLazySuccess() {
        DefaultLoggingScope eager = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.LAZY, sink);
        eager.append(infoLoggingEvent);
        assertTrue(eager.getEvents().isEmpty());
    }

    @Test
    public void testKeepInfoEagerFailureCachesBefore() {
        DefaultLoggingScope lazy = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.EAGER, sink);
        assertTrue(lazy.append(infoLoggingEvent)); // cached
        lazy.failure();
        assertFalse(lazy.getEvents().isEmpty());
    }

    @Test
    public void testKeepInfoEagerFailureNoCacheAfter() {
        DefaultLoggingScope lazy = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.EAGER, sink);
        lazy.failure();
        assertFalse(lazy.append(infoLoggingEvent)); // not cached
        assertTrue(lazy.getEvents().isEmpty());
    }

    @Test
    public void testKeepInfoLazyFailureCachesBefore() {
        DefaultLoggingScope lazy = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.LAZY, sink);
        lazy.append(infoLoggingEvent);
        lazy.failure();
        assertFalse(lazy.getEvents().isEmpty());
    }

    @Test
    public void testKeepInfoLazyFailureCachesAfter() {
        DefaultLoggingScope lazy = new DefaultLoggingScope(infoPredicate, debugPredicate, LoggingScopeFlushMode.LAZY, sink);
        lazy.failure();
        lazy.append(infoLoggingEvent);
        assertFalse(lazy.getEvents().isEmpty());
    }


}
