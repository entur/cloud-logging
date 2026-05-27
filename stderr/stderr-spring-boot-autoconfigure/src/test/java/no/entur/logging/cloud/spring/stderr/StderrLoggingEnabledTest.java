package no.entur.logging.cloud.spring.stderr;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that System.err is redirected to SLF4J when the autoconfiguration is enabled
 * (the default behaviour).
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
public class StderrLoggingEnabledTest {

    @Autowired
    private SystemErrToSlf4jPrintStream systemErrToSlf4jPrintStream;

    private ListAppender<ILoggingEvent> listAppender;
    private ch.qos.logback.classic.Logger stderrLogger;

    @BeforeEach
    public void attachListAppender() {
        stderrLogger = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger("stderr");
        listAppender = new ListAppender<>();
        listAppender.start();
        stderrLogger.addAppender(listAppender);
    }

    @AfterEach
    public void detachListAppender() {
        stderrLogger.detachAppender(listAppender);
        listAppender.stop();
    }

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
        // println(String) is emitted immediately – no explicit flush required
        assertEquals(1, listAppender.list.size());
        assertEquals("Test single-line error message", listAppender.list.get(0).getFormattedMessage());
    }

    @Test
    public void testPrintlnObjectIsForwarded() {
        System.err.println((Object) "Test object message");
        // println(Object) that is not from Throwable.printStackTrace is emitted immediately
        assertEquals(1, listAppender.list.size());
        assertEquals("Test object message", listAppender.list.get(0).getFormattedMessage());
    }

    @Test
    public void testStackTraceIsCombinedIntoSingleLogStatement() {
        new RuntimeException("Intentional test exception – not a real error").printStackTrace();
        // Throwable.printStackTrace only calls println(Object); the accumulated buffer is
        // flushed when flush() is called (or on the next non-printStackTrace println).
        System.err.flush();

        assertEquals(1, listAppender.list.size(),
                "Full stack trace must be combined into exactly one log statement");
        String message = listAppender.list.get(0).getFormattedMessage();
        assertTrue(message.startsWith("java.lang.RuntimeException: Intentional test exception"),
                "Log message must begin with the exception header");
        assertTrue(message.contains("\tat "),
                "Log message must contain at least one stack frame");
    }

    @Test
    public void testChainedExceptionsAreCombined() {
        RuntimeException cause = new RuntimeException("root cause");
        new RuntimeException("wrapper exception", cause).printStackTrace();
        System.err.flush();

        assertEquals(1, listAppender.list.size(),
                "Chained exception stack trace must be combined into exactly one log statement");
        String message = listAppender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("wrapper exception"), "Message must contain the outer exception");
        assertTrue(message.contains("Caused by:"), "Message must contain the cause chain");
        assertTrue(message.contains("root cause"), "Message must contain the root cause");
    }

    @Test
    public void testStackTraceFromCompletedThreadIsFlushedAutomatically() throws InterruptedException {
        Thread worker = new Thread(() ->
                new RuntimeException("auto-flush test exception").printStackTrace()
        );
        worker.start();
        worker.join(); // Wait for the worker thread to terminate

        // The background flusher wakes up every FLUSHER_INTERVAL_MS and treats buffers older
        // than STALE_BUFFER_AGE_MS as stale.  Allow up to 1 second for the flush to happen.
        long deadline = System.currentTimeMillis() + 1_000L;
        while (listAppender.list.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertEquals(1, listAppender.list.size(),
                "Stack trace from a completed thread must be auto-flushed without an explicit flush() call");
        String message = listAppender.list.get(0).getFormattedMessage();
        assertTrue(message.startsWith("java.lang.RuntimeException: auto-flush test exception"),
                "Log message must begin with the exception header");
        assertTrue(message.contains("\tat "),
                "Log message must contain at least one stack frame");
    }
}
