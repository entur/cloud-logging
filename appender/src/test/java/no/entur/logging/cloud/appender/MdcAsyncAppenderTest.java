package no.entur.logging.cloud.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

public class MdcAsyncAppenderTest {

    @AfterEach
    public void cleanUp() {
        MDC.clear();
    }

    /**
     * Build a real, fully-wired {@link LoggingEvent}. The event must carry a {@link
     * ch.qos.logback.classic.LoggerContext} because {@code getMDCPropertyMap()} reads MDC via that
     * context's MDC adapter — the same adapter the static {@link MDC} facade writes to, which is
     * exactly what makes the snapshot observable here.
     */
    private static LoggingEvent realEvent() {
        Logger logger = (Logger) LoggerFactory.getLogger(MdcAsyncAppenderTest.class);
        return new LoggingEvent("fqcn", logger, Level.INFO, "message", null, null);
    }

    /**
     * Characterization test for the reason this appender exists at all: values supplied by the
     * {@link MdcContributer} (e.g. carried in a gRPC {@code Context} rather than the thread-local
     * MDC) must be captured into the event's deferred-processing snapshot at append time, on the
     * calling thread, so they survive the hop to the async worker thread that serializes the log.
     * The caller thread's own MDC must be left exactly as it was.
     *
     * <p>This is what {@code ILoggingEvent#prepareForDeferredProcessing()} freezes, and it can only
     * be exercised with a real {@link LoggingEvent} (a mock never snapshots anything). Guarding it
     * lets us safely swap the forked async base class for stock logback.
     */
    @Test
    public void testContributedMdcIsSnapshotIntoEventAndCallerMdcRestored() {
        MdcAsyncAppender appender = new MdcAsyncAppender();
        appender.setMdcContributer(new MdcContributer() {
            @Override
            public Map<String, String> getMdc() {
                Map<String, String> mdc = new HashMap<>();
                mdc.put("grpcKey", "grpcValue");
                return mdc;
            }
        });

        // The contributed key is NOT present in the calling thread's MDC before append.
        assertThat(MDC.get("grpcKey")).isNull();

        LoggingEvent event = realEvent();

        appender.preprocess(event);

        // The contributed value was frozen into the event's deferred snapshot...
        assertThat(event.getMDCPropertyMap()).containsEntry("grpcKey", "grpcValue");
        // ...without leaking into the calling thread's MDC.
        assertThat(MDC.get("grpcKey")).isNull();
    }

    /**
     * Contributed MDC is merged into the event snapshot alongside the caller thread's own MDC, and
     * is cleaned up afterwards: the caller's unrelated key is left intact and the contributed key
     * does not linger on the caller thread.
     */
    @Test
    public void testContributedMdcMergesWithCallerMdcAndIsCleanedUp() {
        MDC.put("callerKey", "callerValue");

        MdcAsyncAppender appender = new MdcAsyncAppender();
        appender.setMdcContributer(new MdcContributer() {
            @Override
            public Map<String, String> getMdc() {
                Map<String, String> mdc = new HashMap<>();
                mdc.put("grpcKey", "grpcValue");
                return mdc;
            }
        });

        LoggingEvent event = realEvent();
        appender.preprocess(event);

        // The snapshot contains both the caller's MDC and the contributed MDC.
        assertThat(event.getMDCPropertyMap()).containsEntry("callerKey", "callerValue");
        assertThat(event.getMDCPropertyMap()).containsEntry("grpcKey", "grpcValue");
        // The caller thread keeps its own key and never retains the contributed one.
        assertThat(MDC.get("callerKey")).isEqualTo("callerValue");
        assertThat(MDC.get("grpcKey")).isNull();
    }

    @Test
    public void testMdcNotTouchedWhenContributerEmpty() {
        MDC.put("reqId", "original");

        MdcAsyncAppender appender = new MdcAsyncAppender();
        // default MdcContributer returns empty map

        ILoggingEvent event = mock(ILoggingEvent.class);
        appender.preprocess(event);

        assertThat(MDC.get("reqId")).isEqualTo("original");
    }

    @Test
    public void testNullKeySkipped() {
        MdcAsyncAppender appender = new MdcAsyncAppender();
        Map<String, String> mdcWithNullKey = new HashMap<>();
        mdcWithNullKey.put(null, "value");
        appender.setMdcContributer(new MdcContributer() {
            @Override
            public Map<String, String> getMdc() {
                return mdcWithNullKey;
            }
        });

        ILoggingEvent event = mock(ILoggingEvent.class);
        appender.preprocess(event);
    }

    @Test
    public void testNullValueSkipped() {
        MdcAsyncAppender appender = new MdcAsyncAppender();
        Map<String, String> mdcWithNullValue = new HashMap<>();
        mdcWithNullValue.put("reqId", null);
        appender.setMdcContributer(new MdcContributer() {
            @Override
            public Map<String, String> getMdc() {
                return mdcWithNullValue;
            }
        });

        ILoggingEvent event = mock(ILoggingEvent.class);
        appender.preprocess(event);
    }
}
