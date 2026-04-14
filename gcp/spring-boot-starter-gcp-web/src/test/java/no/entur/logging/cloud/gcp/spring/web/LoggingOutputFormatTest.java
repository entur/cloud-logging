package no.entur.logging.cloud.gcp.spring.web;

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
import org.slf4j.Marker;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
public class LoggingOutputFormatTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;
    private DevOpsLogger devOpsLogger;

    @BeforeEach
    public void setUp() {
        logger = (Logger) LoggerFactory.getLogger("test.format");
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        devOpsLogger = DevOpsLoggerFactory.getLogger(logger);
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
        MDC.clear();
    }

    @Test
    public void testInfoEventHasCorrectLevel() {
        logger.info("test info message");

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testErrorEventHasCorrectLevel() {
        logger.error("test error message");

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    public void testWarnEventHasCorrectLevel() {
        logger.warn("test warn message");

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    public void testExceptionIncludedInEvent() {
        RuntimeException exception = new RuntimeException("something went wrong");
        logger.error("test error with exception", exception);

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getThrowableProxy()).isNotNull();
        assertThat(listAppender.list.get(0).getThrowableProxy().getMessage()).isEqualTo("something went wrong");
    }

    @Test
    public void testMdcValueCapturedInEvent() {
        MDC.put("correlationId", "abc-456");
        logger.info("test mdc message");

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getMDCPropertyMap()).containsKey("correlationId");
        assertThat(listAppender.list.get(0).getMDCPropertyMap().get("correlationId")).isEqualTo("abc-456");
    }

    @Test
    public void testDevOpsMarkerPresent() {
        devOpsLogger.errorTellMeTomorrow("test devops marker message");

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        List<Marker> markers = event.getMarkerList();
        assertThat(markers).isNotNull();
        assertThat(markers).isNotEmpty();
        Marker firstMarker = markers.get(0);
        assertThat(firstMarker).isInstanceOf(DevOpsMarker.class);
        DevOpsMarker devOpsMarker = (DevOpsMarker) firstMarker;
        assertThat(devOpsMarker.getDevOpsLevel()).isEqualTo(DevOpsLevel.ERROR_TELL_ME_TOMORROW);
    }
}
