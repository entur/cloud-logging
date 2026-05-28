package no.entur.logging.cloud.spring.stderr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static com.google.common.truth.Truth.assertThat;

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
        assertThat(systemErrToSlf4jPrintStream).isNull();
    }

    @Test
    public void testSystemErrIsNotRedirected() {
        assertThat(System.err).isNotInstanceOf(SystemErrToSlf4jPrintStream.class);
    }
}
