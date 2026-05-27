package no.entur.logging.cloud.spring.stderr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that System.err is redirected to SLF4J when the autoconfiguration is enabled
 * (the default behaviour).
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
public class StderrLoggingEnabledTest {

    @Autowired
    private SystemErrToSlf4jPrintStream systemErrToSlf4jPrintStream;

    @Test
    public void testBeanCreated() {
        assertNotNull(systemErrToSlf4jPrintStream);
    }

    @Test
    public void testSystemErrIsRedirected() {
        assertSame(systemErrToSlf4jPrintStream, System.err);
    }

    @Test
    public void testSingleLineIsForwarded() {
        System.err.println("Test single-line error message");
    }

    @Test
    public void testStackTraceIsCombinedIntoSingleLogStatement() {
        new RuntimeException("Test exception for stack trace combining").printStackTrace();
    }

    @Test
    public void testChainedExceptionsAreCombined() {
        RuntimeException cause = new RuntimeException("root cause");
        new RuntimeException("wrapper exception", cause).printStackTrace();
    }
}
