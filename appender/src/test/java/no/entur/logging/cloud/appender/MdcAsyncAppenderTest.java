package no.entur.logging.cloud.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
