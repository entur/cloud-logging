package no.entur.logging.cloud.spring.ondemand.grpc;

import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
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
        LOGGER.trace("Test trace");
        LOGGER.debug("Test debug");
        LOGGER.info("Test info");
        LOGGER.warn("Test warn");
        LOGGER.error("Test error");
    }
}
