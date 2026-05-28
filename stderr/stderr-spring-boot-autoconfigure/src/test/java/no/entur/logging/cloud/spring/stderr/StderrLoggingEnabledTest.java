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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

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
        assertThat(systemErrToSlf4jPrintStream).isNotNull();
    }

    @Test
    public void testSystemErrIsRedirected() {
        assertThat(System.err).isSameInstanceAs(systemErrToSlf4jPrintStream);
    }

    @Test
    public void testSingleLineIsForwarded() {
        System.err.println("Test single-line error message");
        // println(String) is emitted immediately – no explicit flush required
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getFormattedMessage()).isEqualTo("Test single-line error message");
    }

    @Test
    public void testPrintlnObjectIsForwarded() {
        System.err.println((Object) "Test object message");
        // println(Object) that is not from Throwable.printStackTrace is emitted immediately
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getFormattedMessage()).isEqualTo("Test object message");
    }

    @Test
    public void testStackTraceIsCombinedIntoSingleLogStatement() {
        new RuntimeException("Intentional test exception – not a real error").printStackTrace();
        // Throwable.printStackTrace only calls println(Object); the accumulated buffer is
        // flushed when flush() is called (or on the next non-printStackTrace println).
        System.err.flush();

        assertWithMessage("Full stack trace must be combined into exactly one log statement")
                .that(listAppender.list).hasSize(1);
        String message = listAppender.list.get(0).getFormattedMessage();
        assertWithMessage("Log message must begin with the exception header")
                .that(message).startsWith("java.lang.RuntimeException: Intentional test exception");
        assertWithMessage("Log message must contain at least one stack frame")
                .that(message).contains("\tat ");
    }

    @Test
    public void testChainedExceptionsAreCombined() {
        RuntimeException cause = new RuntimeException("root cause");
        new RuntimeException("wrapper exception", cause).printStackTrace();
        System.err.flush();

        assertWithMessage("Chained exception stack trace must be combined into exactly one log statement")
                .that(listAppender.list).hasSize(1);
        String message = listAppender.list.get(0).getFormattedMessage();
        assertWithMessage("Message must contain the outer exception").that(message).contains("wrapper exception");
        assertWithMessage("Message must contain the cause chain").that(message).contains("Caused by:");
        assertWithMessage("Message must contain the root cause").that(message).contains("root cause");
    }

    @Test
    public void testStackTraceFromCompletedThreadIsFlushedAutomatically() throws InterruptedException {
        Thread worker = new Thread(() ->
                new RuntimeException("auto-flush test exception").printStackTrace()
        );
        worker.start();
        worker.join(); // Wait for the worker thread to terminate

        // The background flusher wakes up every FLUSHER_INTERVAL_MS and treats buffers older
        // than STALE_BUFFER_AGE_MS as stale.  Allow up to 3 seconds to accommodate slow CI.
        long deadline = System.currentTimeMillis() + 3_000L;
        while (listAppender.list.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertWithMessage("Stack trace from a completed thread must be auto-flushed without an explicit flush() call")
                .that(listAppender.list).hasSize(1);
        String message = listAppender.list.get(0).getFormattedMessage();
        assertWithMessage("Log message must begin with the exception header")
                .that(message).startsWith("java.lang.RuntimeException: auto-flush test exception");
        assertWithMessage("Log message must contain at least one stack frame")
                .that(message).contains("\tat ");
    }

    @Test
    public void testConcurrentStackTracesFromMultipleThreadsAreAllCaptured() throws InterruptedException {
        int threadCount = 8;
        List<Thread> workers = new ArrayList<>(threadCount);
        for (int threadNumber = 0; threadNumber < threadCount; threadNumber++) {
            int threadIndex = threadNumber;
            Thread worker = new Thread(() ->
                    new RuntimeException("concurrent-ex-" + threadIndex).printStackTrace()
            );
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }

        System.err.flush();

        assertWithMessage("Every thread stack trace must produce exactly one combined log statement")
                .that(listAppender.list).hasSize(threadCount);
        for (int threadNumber = 0; threadNumber < threadCount; threadNumber++) {
            String exceptionId = "concurrent-ex-" + threadNumber;
            assertWithMessage("Missing stack trace output for " + exceptionId)
                    .that(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains(exceptionId)))
                    .isTrue();
        }
    }

    @Test
    public void testDestroyFlushesPendingOutputWithoutLoss() throws Exception {
        PrintStream previous = System.err;
        SystemErrToSlf4jPrintStream local = new SystemErrToSlf4jPrintStream(stderrLogger, org.slf4j.event.Level.ERROR, previous);
        System.setErr(local);
        try {
            Thread worker = new Thread(() ->
                    new RuntimeException("destroy-flush-ex").printStackTrace()
            );
            worker.start();
            worker.join();

            local.destroy();
        } finally {
            if (System.err != previous) {
                System.setErr(previous);
            }
        }

        assertWithMessage("Destroy must flush pending stderr output").that(listAppender.list).isNotEmpty();
        assertWithMessage("Expected stack trace emitted during destroy()")
                .that(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("destroy-flush-ex")))
                .isTrue();
    }

    @Test
    public void testRepeatedPrintStackTraceOnSameThreadFlushedAsSeparateEvents() throws InterruptedException {
        // Two back-to-back printStackTrace calls on the same thread.  When the header line of
        // the second trace arrives it is detected as a new-trace start and the first trace is
        // flushed synchronously – no artificial delay is needed.
        Thread worker = new Thread(() -> {
            new RuntimeException("repeated-ex-1").printStackTrace();
            new RuntimeException("repeated-ex-2").printStackTrace();
        });
        worker.start();
        worker.join();

        // The first trace is flushed synchronously when the second trace starts.
        // The second trace is flushed by the background stale-flusher after the thread exits.
        long deadline = System.currentTimeMillis() + 3_000L;
        while (listAppender.list.size() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertWithMessage("Each repeated printStackTrace call must produce its own log event")
                .that(listAppender.list).hasSize(2);
        assertWithMessage("First event must contain repeated-ex-1")
                .that(listAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("repeated-ex-1")))
                .isTrue();
        assertWithMessage("Second event must contain repeated-ex-2")
                .that(listAppender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("repeated-ex-2")))
                .isTrue();
    }
}
