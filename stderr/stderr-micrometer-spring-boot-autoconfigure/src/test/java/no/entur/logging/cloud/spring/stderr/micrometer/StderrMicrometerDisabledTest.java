package no.entur.logging.cloud.spring.stderr.micrometer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies that System.err is NOT intercepted when the autoconfiguration is explicitly disabled.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
@TestPropertySource(properties = {"entur.logging.stderr.micrometer.enabled=false"})
public class StderrMicrometerDisabledTest {

    @Autowired(required = false)
    private SystemErrMicrometerPrintStream systemErrMicrometerPrintStream;

    @Test
    public void testBeanNotCreated() {
        assertThat(systemErrMicrometerPrintStream).isNull();
    }

    @Test
    public void testSystemErrIsNotIntercepted() {
        assertThat(System.err).isNotInstanceOf(SystemErrMicrometerPrintStream.class);
    }
}
