package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CompositeConsoleAsyncAppenderLoggingTest {

    @AfterEach
    public void cleanUp() {
        MDC.clear();
    }

    private static LoggingEvent realEvent() {
        Logger logger = (Logger) LoggerFactory.getLogger(CompositeConsoleAsyncAppenderLoggingTest.class);
        return new LoggingEvent("fqcn", logger, Level.INFO, "message", null, null);
    }

    @Test
    public void testValidatorRunsAfterPreprocessAndPropagatesFromDoAppend() {
        CompositeConsoleAsyncAppenderLogging appender = new CompositeConsoleAsyncAppenderLogging();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.setName("TEST");
        appender.addAppender(new NoopAppender());
        appender.setMdcContributer(new no.entur.logging.cloud.appender.MdcContributer() {
            @Override
            public Map<String, String> getMdc() {
                Map<String, String> mdc = new HashMap<>();
                mdc.put("grpcKey", "grpcValue");
                return mdc;
            }
        });
        appender.setValidator(event -> {
            assertThat(event.getMDCPropertyMap()).containsEntry("grpcKey", "grpcValue");
            throw new IllegalStateException("boom");
        });
        appender.start();

        IllegalStateException exception;
        try {
            exception = assertThrows(IllegalStateException.class, () -> appender.doAppend(realEvent()));
        } finally {
            appender.stop();
        }

        assertThat(exception).hasMessageThat().contains("boom");
        assertThat(MDC.get("grpcKey")).isNull();
    }

    private static class NoopAppender extends AppenderBase<ILoggingEvent> {

        private NoopAppender() {
            start();
        }

        @Override
        protected void append(ILoggingEvent eventObject) {
        }
    }
}
