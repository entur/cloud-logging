package no.entur.logging.cloud.azure.spring.web.test;

import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext

@EnableAutoConfiguration
public class LoadContextLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoadContextLoggingTest.class);

    @Test
    public void testHumanReadablePlain() {
        CompositeConsoleOutputControl.useHumanReadablePlainEncoder();

        LOGGER.trace("Test trace message");
        LOGGER.debug("Test debug message");
        LOGGER.info("Test info message");
        LOGGER.warn("Test warn message");
        LOGGER.error("Test error message");

        LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message");
        LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message");
        LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message");
    }

    @Test
    public void testHumanReadableJson() throws InterruptedException {
        try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
            LOGGER.trace("Test trace message");
            LOGGER.debug("Test debug message");
            LOGGER.info("Test info message");
            LOGGER.warn("Test warn message");
            LOGGER.error("Test error message");

            LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message");
            LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message");
            LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message");
        }
    }

    @Test
    public void testMachineReadableJson() throws IOException {
        try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {

            LOGGER.trace("Test trace message");
            LOGGER.debug("Test debug message");
            LOGGER.info("Test info message");
            LOGGER.warn("Test warn message");
            LOGGER.error("Test error message");

            LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message");
            LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message");
            LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message");
        }
    }

    @AfterEach
    public void waitForFlush()  throws Exception {
        Thread.sleep(100);

        CompositeConsoleOutputControl.useHumanReadablePlainEncoder();
    }

}
