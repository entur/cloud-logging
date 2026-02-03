package no.entur.logging.cloud.gcp.spring.web;

import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
public class LoadContextLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoadContextLoggingTest.class);

    @Test
    public void testMachineReadableJson() throws IOException {
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
    public void testMachineReadableJsonWithException() throws IOException {
        IOException e = new IOException("Something went wrong");

        LOGGER.trace("Test trace message", e);
        LOGGER.debug("Test debug message", e);
        LOGGER.info("Test info message", e);
        LOGGER.warn("Test warn message", e);
        LOGGER.error("Test error message", e);

        LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message", e);
        LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message", e);
        LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message", e);
    }

}
