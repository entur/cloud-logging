package no.entur.logging.cloud.spring.stderr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Verifies that System.err is NOT redirected when the autoconfiguration is explicitly disabled.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
@TestPropertySource(properties = {"entur.logging.stderr.enabled=false"})
public class StderrLoggingDisabledTest {

    @Autowired(required = false)
    private SystemErrToSlf4jPrintStream systemErrToSlf4jPrintStream;

    @Test
    public void testBeanNotCreated() {
        assertNull(systemErrToSlf4jPrintStream);
    }

    @Test
    public void testSystemErrIsNotRedirected() {
        assertNotSame(SystemErrToSlf4jPrintStream.class, System.err.getClass());
    }
}
