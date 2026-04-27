package no.entur.logging.cloud.azure.spring.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
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
public class MdcLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(MdcLoggingTest.class);

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setupAppender() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(MdcLoggingTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logbackLogger.addAppender(listAppender);
    }

    @AfterEach
    public void teardown() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(MdcLoggingTest.class);
        logbackLogger.detachAppender(listAppender);
        listAppender.stop();
        MDC.clear();
    }

    @Test
    public void testMdcValueSetBeforeLogging() {
        MDC.put("correlationId", "azure-test-123");
        LOGGER.info("Test info message with MDC correlationId");

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getMDCPropertyMap()).containsKey("correlationId");
        assertThat(listAppender.list.get(0).getMDCPropertyMap().get("correlationId")).isEqualTo("azure-test-123");
    }
}
