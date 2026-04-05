package no.entur.logging.cloud.azure.spring.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
public class LoggingOutputFormatTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoggingOutputFormatTest.class);

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setupAppender() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(LoggingOutputFormatTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logbackLogger.addAppender(listAppender);
    }

    @AfterEach
    public void teardownAppender() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(LoggingOutputFormatTest.class);
        logbackLogger.detachAppender(listAppender);
        listAppender.stop();
        MDC.clear();
    }

    @Test
    public void testInfoEventHasCorrectLevel() {
        LOGGER.info("Info message");

        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testErrorEventHasCorrectLevel() {
        LOGGER.error("Error message");

        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    public void testExceptionIncludedInEvent() {
        LOGGER.error("Error with exception", new RuntimeException("test exception"));

        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getThrowableProxy()).isNotNull();
    }

    @Test
    public void testMdcValueCapturedInEvent() {
        MDC.put("traceId", "abc-123");
        LOGGER.info("Message with MDC");

        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getMDCPropertyMap()).containsKey("traceId");
    }

    @Test
    public void testDevOpsMarkerPresent() {
        LOGGER.errorTellMeTomorrow("Tell me tomorrow error");

        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getMarker()).isInstanceOf(DevOpsMarker.class);
        DevOpsMarker marker = (DevOpsMarker) event.getMarker();
        assertThat(marker.getDevOpsLevel()).isEqualTo(DevOpsLevel.ERROR_TELL_ME_TOMORROW);
    }

}
