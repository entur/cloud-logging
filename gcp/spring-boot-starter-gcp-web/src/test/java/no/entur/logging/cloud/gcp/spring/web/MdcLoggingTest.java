package no.entur.logging.cloud.gcp.spring.web;

import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
public class MdcLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(MdcLoggingTest.class);

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void testMdcValueIncludedInLog() {
        MDC.put("correlationId", "test-123");
        LOGGER.info("Test info message with MDC correlationId");
    }
}
