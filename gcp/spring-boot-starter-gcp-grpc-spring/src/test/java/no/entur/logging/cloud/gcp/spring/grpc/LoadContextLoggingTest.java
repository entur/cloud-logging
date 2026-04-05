package no.entur.logging.cloud.gcp.spring.grpc;

import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = DemoApplication.class)
@DirtiesContext
@EnableAutoConfiguration
@TestPropertySource(properties = {
    "spring.grpc.server.port=-1"
})
public class LoadContextLoggingTest {
    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoadContextLoggingTest.class);

    @Test
    public void testContextLoads() {
        // context loaded successfully
    }

    @Test
    public void testLoggingAtAllLevels() {
        LOGGER.trace("Test trace message");
        LOGGER.debug("Test debug message");
        LOGGER.info("Test info message");
        LOGGER.warn("Test warn message");
        LOGGER.error("Test error message");
        LOGGER.errorTellMeTomorrow("Test error tell me tomorrow");
        LOGGER.errorInterruptMyDinner("Test error interrupt my dinner");
        LOGGER.errorWakeMeUpRightNow("Test error wake me up right now");
    }

    @Test
    public void testLoggingWithException() {
        Exception e = new RuntimeException("Test exception");
        LOGGER.error("Error with exception", e);
    }
}
