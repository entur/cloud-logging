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
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    public void testLoggingFromWithinValidatorDoesNotRecurse() {
        CompositeConsoleAsyncAppenderLogging appender = new CompositeConsoleAsyncAppenderLogging();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.setName("TEST");
        appender.addAppender(new NoopAppender());

        // e.g. a marker or getter which the validator replays, which itself logs
        AtomicInteger validations = new AtomicInteger();
        appender.setValidator(event -> {
            validations.incrementAndGet();
            appender.doAppend(realEvent());
        });
        appender.start();

        try {
            appender.doAppend(realEvent());
        } finally {
            appender.stop();
        }

        assertThat(validations.get()).isEqualTo(1);
    }

    @Test
    public void testValidatorRunsAgainAfterEarlierValidationThrew() {
        CompositeConsoleAsyncAppenderLogging appender = new CompositeConsoleAsyncAppenderLogging();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.setName("TEST");
        appender.addAppender(new NoopAppender());

        AtomicInteger validations = new AtomicInteger();
        appender.setValidator(event -> {
            validations.incrementAndGet();
            throw new IllegalStateException("boom");
        });
        appender.start();

        try {
            assertThrows(IllegalStateException.class, () -> appender.doAppend(realEvent()));
            assertThrows(IllegalStateException.class, () -> appender.doAppend(realEvent()));
        } finally {
            appender.stop();
        }

        assertThat(validations.get()).isEqualTo(2);
    }

    @Test
    public void testEventIsStillAppendedWhenValidationFails() {
        CompositeConsoleAsyncAppenderLogging appender = new CompositeConsoleAsyncAppenderLogging();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.setName("TEST");
        CountingAppender counting = new CountingAppender();
        appender.addAppender(counting);
        appender.setValidator(event -> {
            throw new IllegalStateException("boom");
        });
        appender.start();

        try {
            assertThrows(IllegalStateException.class, () -> appender.doAppend(realEvent()));
            // the async queue is drained on stop
        } finally {
            appender.stop();
        }

        assertThat(counting.count.get()).isEqualTo(1);
    }

    private static class CountingAppender extends AppenderBase<ILoggingEvent> {

        private final AtomicInteger count = new AtomicInteger();

        private CountingAppender() {
            start();
        }

        @Override
        protected void append(ILoggingEvent eventObject) {
            count.incrementAndGet();
        }
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
