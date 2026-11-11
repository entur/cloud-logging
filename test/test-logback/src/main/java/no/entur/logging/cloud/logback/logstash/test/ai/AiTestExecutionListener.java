package no.entur.logging.cloud.logback.logstash.test.ai;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AsyncAppenderBase;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Iterator;

/**
 * AI mode: tells {@link AiTestRun} which tests ran on which thread and when, and which failed, and when a test plan has finished,
 * has the failures file written (see {@link FailuresFileAppender}). Registered in <code>META-INF/services</code>, so Gradle and Maven
 * pick it up without configuration; does nothing unless AI mode is enabled. The test engine calls it on the thread that runs the test.
 */
public class AiTestExecutionListener implements TestExecutionListener {

    private static final long QUEUE_WAIT_MILLIS = 2_000;

    private final AiTestRun run;

    private final boolean enabled;

    public AiTestExecutionListener() {
        this(AiTestRun.current(), AiLoggingControl.isAiEnabled() && AiLoggingControl.isFailuresEnabled());
    }

    AiTestExecutionListener(AiTestRun run, boolean enabled) {
        this.run = run;
        this.enabled = enabled;
    }

    @Override
    public void executionStarted(TestIdentifier identifier) {
        if (!enabled) {
            return;
        }
        TestSource source = identifier.getSource().orElse(null);
        run.testStarted(identifier.getUniqueId(), identifier.getParentId().orElse(null), name(identifier, source), identifier.isTest(),
                source instanceof ClassSource, Thread.currentThread().getName(), now());
    }

    @Override
    public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
        if (!enabled) {
            return;
        }
        boolean failed = result.getStatus() == TestExecutionResult.Status.FAILED;
        run.testFinished(identifier.getUniqueId(), now(), failed, failed ? result.getThrowable().map(AiTestExecutionListener::failure).orElse(null) : null);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (!enabled) {
            return;
        }
        // events are written by an async appender: let the last ones of the run arrive first
        waitForLogging();
        FailuresFileAppender.writeReport(run);
    }

    /** <code>SimpleClassName > method()</code> for test methods, the simple class name for classes, otherwise the display name. */
    static String name(TestIdentifier identifier, TestSource source) {
        if (source instanceof MethodSource method) {
            return simpleName(method.getClassName()) + " > " + identifier.getDisplayName();
        }
        if (source instanceof ClassSource clazz) {
            return simpleName(clazz.getClassName());
        }
        return identifier.getDisplayName();
    }

    private static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    /** The first line of the failure: the message of an assertion, or the exception's simple class name and message. */
    static String failure(Throwable throwable) {
        String message = throwable.getMessage();
        String firstLine = message == null ? "" : message.strip().lines().findFirst().orElse("");
        if (throwable instanceof AssertionError && !firstLine.isEmpty()) {
            return cut(firstLine);
        }
        String name = throwable.getClass().getSimpleName();
        return cut(firstLine.isEmpty() ? name : name + ": " + firstLine);
    }

    private static String cut(String text) {
        return text.length() > AiTestRun.MAX_LINE_LENGTH ? text.substring(0, AiTestRun.MAX_LINE_LENGTH) + "..." : text;
    }

    /** At the precision of logging events (logback keeps their time at full precision), so that an event is not taken for the next test's. */
    private static Instant now() {
        return Instant.now();
    }

    private static void waitForLogging() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
            return;
        }
        Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        long deadline = System.nanoTime() + QUEUE_WAIT_MILLIS * 1_000_000;
        for (Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders(); it.hasNext(); ) {
            if (it.next() instanceof AsyncAppenderBase<?> async) {
                while (async.getNumberOfElementsInQueue() > 0 && System.nanoTime() < deadline) {
                    sleep(10);
                }
            }
        }
        // the worker may still be writing the last event it took from the queue
        sleep(50);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
