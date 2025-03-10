package no.entur.logging.cloud.gcp.spring.ondemand;

import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=false"})
public class DisabledTest {

    @Autowired(required = false)
    @Qualifier("enabled")
    private String enabled;

    @Autowired(required = false)
    @Qualifier("disabled")
    private String disabled;

    @Test
    public void testEnabled() throws IOException {
        assertNotNull(disabled);
        assertNull(enabled);
    }

}
