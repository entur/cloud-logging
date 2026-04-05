package no.entur.logging.cloud.azure.spring.grpc;

import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
@TestPropertySource(properties = {
    "spring.grpc.server.port=-1"
})
public class LoadContextLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoadContextLoggingTest.class);

    @Test
    public void testContextLoads() {}

    @Test
    public void testLoggingAtAllLevels() {
        LOGGER.trace("Test trace message");
        LOGGER.debug("Test debug message");
        LOGGER.info("Test info message");
        LOGGER.warn("Test warn message");
        LOGGER.error("Test error message");
        LOGGER.errorTellMeTomorrow("Tell me tomorrow");
        LOGGER.errorInterruptMyDinner("Interrupt my dinner");
        LOGGER.errorWakeMeUpRightNow("Wake me up right now");
    }

    @Test
    public void testLoggingWithException() {
        LOGGER.error("Error with exception", new RuntimeException("Test exception"));
    }
}
