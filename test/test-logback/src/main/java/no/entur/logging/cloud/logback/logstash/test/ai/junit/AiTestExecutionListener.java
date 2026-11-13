package no.entur.logging.cloud.logback.logstash.test.ai.junit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAsyncAppenderLogging;
import no.entur.logging.cloud.logback.logstash.test.ai.AiFiles;
import no.entur.logging.cloud.logback.logstash.test.ai.AiLoggingControl;
import no.entur.logging.cloud.logback.logstash.test.ai.AiTestEvents;
import no.entur.logging.cloud.logback.logstash.test.ai.AiTestMarker;
import no.entur.logging.cloud.logback.logstash.test.ai.AiTestThread;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiText;

/**
 * AI mode: tells which tests failed and where their log lines are. Registered with the JUnit Platform through
 * <code>META-INF/services</code>, so it is active in every test run (Gradle, Maven, IDE) with this library on the test classpath, and
 * does nothing unless <code>-Dentur.logging.ai.enabled=true</code>.
 * <p>
 * <b>Summary file</b> (<code>&lt;file&gt;.summary</code>, shared by all test JVMs): an entry per failed test (or test class, if its
 * set-up or tear-down failed) with the exception, the first frame which is not framework code and commands printing the log lines of
 * the test, and a line with the number of tests per test JVM:
 * <pre>
 * ---- JVM pid 4711 in /work/app, logs /tmp/logs/test.4711.compact ----
 * FAILED DocumentEndpointTest.returnsNotFound() #t17
 *   org.opentest4j.AssertionFailedError: expected: &lt;404&gt; but was: &lt;500&gt;
 *   at no.entur.DocumentEndpointTest.returnsNotFound(DocumentEndpointTest.java:42)
 *   WARN/ERROR logged during the test:
 *     ERROR DocumentService #1a2b3c4d5e6f : Unable to load document 7 | java.io.IOException: timeout | at no.entur.DocumentStore.load(DocumentStore.java:88)
 *   logs: sed -n '/#t17 start/,/#t17 end/p' /tmp/logs/test.4711.compact
 *   class logs (set-up, Spring context start): sed -n '/#t12 start/,/#t12 end/p' /tmp/logs/test.4711.compact
 * ---- JVM pid 4711: 118 tests, 1 failed, 0 aborted, 2 skipped ----
 * </pre>
 * <b>Test markers</b> (see {@link AiTestMarker}): a line at the start and at the end of every test and test class, in the compact and
 * the full file of this JVM. They are queued like log events, so they are in order with the events of the test thread. The test
 * instance is created before the test starts (and with it the Spring application context of the first test of a class), so those
 * events are in the lines of the class and not of the test.
 * <p>
 * <b>Test threads</b> (see {@link AiTestThread}): events logged on the thread of a test are written with its token, and events of
 * other threads with the tokens of all tests running then. If other tests ran at the same time (JUnit parallel execution), the lines
 * between the markers of a test are not only its own, and the entry has a <code>grep '#t17 '</code> command instead.
 * <p>
 * Never fails a test run: any error in here turns the listener off.
 */
public class AiTestExecutionListener implements TestExecutionListener {

    private static final int MAX_MESSAGE = 500;
    private static final int MAX_NAME = 200;
    private static final int MAX_FAILURES = 1000;

    /** What ran in this JVM, per summary file: the test plans of a JVM share a line in the summary file. */
    private static final class Totals {
        int tests;
        int failed;
        int aborted;
        int skipped;
        int notRun;
        int failedContainers;
        boolean fileCreated;
        boolean headerWritten;
        String header = "";
    }

    private static final Map<Path, Totals> TOTALS = new ConcurrentHashMap<>();

    static {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> List.copyOf(TOTALS.keySet()).forEach(AiTestExecutionListener::writeTotals),
                    "entur-logging-ai-summary"));
        } catch (RuntimeException e) {
            // no line with the number of tests then
        }
    }

    // unique within the files of this JVM, whatever number of listeners and test plans
    private static final AtomicInteger TOKENS = new AtomicInteger();

    /** A started test or test class. */
    private static final class Mark {
        final String uniqueId;
        final String token;
        final String name;
        final boolean test;
        // whether the start marker was queued, so that there are lines to point to
        boolean written;
        // the thread it was started on, and the token of that thread before (the test class, usually)
        final Thread thread = Thread.currentThread();
        String previous;
        // another test (for a class: another class) ran at the same time, so the lines between the markers are not only its own
        volatile boolean overlapped;

        Mark(String uniqueId, String token, String name, boolean test) {
            this.uniqueId = uniqueId;
            this.token = token;
            this.name = name;
            this.test = test;
        }

        String token() {
            return token;
        }

        String name() {
            return name;
        }

        boolean written() {
            return written;
        }

        /** A test and its class, or a class and a class nested in it, do not overlap: the lines of one are within those of the other. */
        boolean contains(Mark other) {
            return other.uniqueId.startsWith(uniqueId) || uniqueId.startsWith(other.uniqueId);
        }
    }

    private final Path summaryFile;
    private final String compactFile;
    private final BooleanSupplier aiFilesWritten;
    private final Supplier<ILoggerFactory> loggerFactory;

    private final long pid = ProcessHandle.current().pid();
    private final Map<String, Mark> marks = new ConcurrentHashMap<>();
    // the running tests and test classes, to tell which of them overlap
    private final Set<Mark> running = ConcurrentHashMap.newKeySet();
    // tests which were started or skipped, to tell how many tests of a failed class did not run
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    // failures by exception and frame, to say 'same failure as' instead of repeating them
    private final Map<String, String> failures = new ConcurrentHashMap<>();

    private final AtomicInteger tests = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger aborted = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger notRun = new AtomicInteger();
    private final AtomicInteger failedContainers = new AtomicInteger();

    private volatile boolean enabled;
    private volatile TestPlan testPlan;
    private final Totals totals;

    public AiTestExecutionListener() {
        this(AiLoggingControl.isAiEnabled(), AiLoggingControl.aiFile(), AiLoggingControl.aiJvmFile(), AiLoggingControl::isAiActive, LoggerFactory::getILoggerFactory);
    }

    /**
     * @param file           the AI files without extension (<code>entur.logging.ai.file</code>), for the summary file
     * @param jvmFile        the compact and full file of this JVM without extension
     * @param aiFilesWritten whether logging goes to the AI files, so that test markers can be written
     */
    AiTestExecutionListener(boolean enabled, String file, String jvmFile, BooleanSupplier aiFilesWritten, Supplier<ILoggerFactory> loggerFactory) {
        this.enabled = enabled;
        this.summaryFile = Path.of(file + ".summary");
        this.compactFile = jvmFile + ".compact";
        this.aiFilesWritten = aiFilesWritten;
        this.loggerFactory = loggerFactory;
        this.totals = TOTALS.computeIfAbsent(summaryFile, path -> new Totals());
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.testPlan = testPlan;
        if (enabled) {
            try {
                // configures logging now (logback-test.xml), so that the tests which run before anything is logged have markers too
                loggerFactory.get();
                AiTestEvents.enable();
            } catch (Exception | LinkageError e) {
                enabled = false;
            }
        }
    }

    @Override
    public void executionStarted(TestIdentifier identifier) {
        if (!enabled) {
            return;
        }
        try {
            if (identifier.isTest()) {
                seen.add(identifier.getUniqueId());
            }
            if (identifier.isTest() || isClass(identifier)) {
                Mark mark = new Mark(identifier.getUniqueId(), "t" + TOKENS.incrementAndGet(), nameOf(identifier), identifier.isTest());
                synchronized (running) {
                    for (Mark other : running) {
                        if (other.test == mark.test && !other.contains(mark)) {
                            other.overlapped = true;
                            mark.overlapped = true;
                        }
                    }
                    running.add(mark);
                }
                marks.put(mark.uniqueId, mark);
                mark.written = write(AiTestMarker.start(mark.token, mark.name));
                if (totals.header.isEmpty()) {
                    totals.header = header();
                }
                // after the marker, so that the events logged on this thread from now on have the token
                mark.previous = AiTestThread.set(mark.token);
                if (mark.test) {
                    AiTestThread.started(mark.token);
                }
            }
        } catch (Exception | LinkageError e) {
            enabled = false;
        }
    }

    @Override
    public void executionSkipped(TestIdentifier identifier, String reason) {
        if (!enabled) {
            return;
        }
        try {
            if (identifier.isTest()) {
                seen.add(identifier.getUniqueId());
                skipped.incrementAndGet();
            }
            skipped.addAndGet(markTestsSeen(identifier));
        } catch (Exception | LinkageError e) {
            enabled = false;
        }
    }

    @Override
    public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
        if (!enabled) {
            return;
        }
        try {
            TestExecutionResult.Status status = result.getStatus();
            boolean failure = status == TestExecutionResult.Status.FAILED;
            if (identifier.isTest()) {
                seen.add(identifier.getUniqueId());
                tests.incrementAndGet();
                if (failure) {
                    failed.incrementAndGet();
                } else if (status == TestExecutionResult.Status.ABORTED) {
                    aborted.incrementAndGet();
                }
            } else if (failure) {
                failedContainers.incrementAndGet();
            }
            // the mark of this identifier is kept until its entry is written (a test class is the 'class logs' of its own entry too)
            Mark mark = marks.get(identifier.getUniqueId());
            if (mark != null) {
                running.remove(mark);
                if (mark.test) {
                    AiTestThread.finished(mark.token);
                }
                if (mark.thread == Thread.currentThread() && mark.token.equals(AiTestThread.current())) {
                    AiTestThread.restore(mark.previous);
                }
            }
            if (mark != null && mark.written()) {
                String outcome = switch (status) {
                    case SUCCESSFUL -> "passed";
                    case ABORTED -> "aborted";
                    case FAILED -> "FAILED";
                };
                write(AiTestMarker.end(mark.token(), mark.name(), outcome, failure));
            }
            if (failure) {
                // tests which did not run because the set-up of their class failed
                int missing = identifier.isTest() ? 0 : markTestsSeen(identifier);
                notRun.addAndGet(missing);
                writeFailure(identifier, mark, result.getThrowable().orElse(null), missing);
            }
            if (mark != null) {
                AiTestEvents.of(mark.token(), true); // of a test which passed, or already written
            }
            marks.remove(identifier.getUniqueId());
        } catch (Exception | LinkageError e) {
            enabled = false;
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (!enabled) {
            return;
        }
        try {
            // a test JVM may run several test plans (Maven runs one per test class in a reused fork): one line for all of them, at exit
            synchronized (totals) {
                totals.tests += tests.getAndSet(0);
                totals.failed += failed.getAndSet(0);
                totals.aborted += aborted.getAndSet(0);
                totals.skipped += skipped.getAndSet(0);
                totals.notRun += notRun.getAndSet(0);
                totals.failedContainers += failedContainers.getAndSet(0);
            }
            marks.clear();
            running.clear();
            AiTestThread.reset();
            AiTestEvents.reset();
            seen.clear();
            failures.clear();
        } catch (Exception | LinkageError e) {
            enabled = false;
        }
    }

    /** Writes the line with the number of tests of this JVM to the summary file (at JVM exit). */
    static void writeTotals(Path summaryFile) {
        Totals totals = TOTALS.remove(summaryFile);
        if (totals == null) {
            return;
        }
        try {
            synchronized (totals) {
                if (totals.tests + totals.skipped + totals.notRun + totals.failedContainers == 0) {
                    return; // nothing ran, for example a launcher within a test
                }
                StringBuilder line = new StringBuilder("---- JVM pid ").append(ProcessHandle.current().pid());
                if (!totals.headerWritten) {
                    line.append(totals.header);
                }
                line.append(": ").append(totals.tests).append(" tests, ").append(totals.failed).append(" failed, ")
                        .append(totals.aborted).append(" aborted, ").append(totals.skipped).append(" skipped");
                if (totals.notRun > 0) {
                    line.append(", ").append(totals.notRun).append(" not run");
                }
                if (totals.failedContainers > 0) {
                    line.append(", ").append(totals.failedContainers).append(" failed test classes (set-up or tear-down)");
                }
                line.append(" ----\n");
                append(summaryFile, totals, line.toString());
            }
        } catch (Exception | LinkageError e) {
            // nothing to be done at exit
        }
    }

    // ---- test markers

    /** Queues the marker with the log events, if they go to the AI files. Returns false if not, as there are no lines to point to then. */
    private boolean write(AiTestMarker marker) {
        // the logger factory first: asking for it configures logging, which starts the AI files
        if (!(loggerFactory.get() instanceof LoggerContext context) || !aiFilesWritten.getAsBoolean()) {
            return false;
        }
        Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        boolean written = false;
        Iterator<Appender<ILoggingEvent>> appenders = root.iteratorForAppenders();
        while (appenders.hasNext()) {
            if (appenders.next() instanceof CompositeConsoleAsyncAppenderLogging appender && appender.isStarted()) {
                LoggingEvent event = new LoggingEvent(AiTestExecutionListener.class.getName(), root, Level.INFO, marker.getLine(), null, null);
                event.addMarker(marker);
                event.setMDCPropertyMap(Map.of()); // complete as it is: not prepared for deferred processing like logged events
                appender.putUnobserved(event);
                written = true;
            }
        }
        return written;
    }

    // ---- summary file

    private void writeFailure(TestIdentifier identifier, Mark mark, Throwable throwable, int missing) throws IOException {
        String name = mark != null ? mark.name() : nameOf(identifier);
        StringBuilder entry = new StringBuilder("FAILED ").append(name);
        if (!identifier.isTest()) {
            entry.append(isClass(identifier) ? " (test class set-up or tear-down)" : " (container)");
        }
        if (mark != null) {
            entry.append(" #").append(mark.token());
        }
        if (missing > 0) {
            entry.append(", ").append(missing).append(" tests not run");
        }

        Throwable root = rootCause(throwable);
        String key = throwable == null ? null : throwable.getClass().getName() + ':' + throwable.getMessage() + '|' + root.getClass().getName()
                + ':' + root.getMessage() + '|' + frame(throwable) + '|' + frame(root);
        String same = key == null ? null : failures.get(key);
        if (same != null) {
            entry.append(": same failure as ").append(same).append('\n');
        } else {
            entry.append('\n');
            if (key != null && failures.size() < MAX_FAILURES) {
                failures.put(key, mark != null ? name + " #" + mark.token() : name);
            }
            if (throwable != null) {
                describe(entry, "", throwable);
                if (root != throwable) {
                    describe(entry, "root cause: ", root);
                }
            }
        }
        // what the test logged at WARN and ERROR, so that the cause can be seen without running a command
        List<String> events = mark == null ? List.of() : AiTestEvents.of(mark.token(), true);
        Mark parent = parentMark(identifier);
        String heading = identifier.isTest() ? "  WARN/ERROR logged during the test" : "  WARN/ERROR logged during the set-up or tear-down";
        if (events.isEmpty() && parent != null) {
            // for example a Spring context which failed to start: logged before the test started
            events = AiTestEvents.of(parent.token(), false);
            heading = "  WARN/ERROR logged during the set-up of the test class";
        }
        if (!events.isEmpty()) {
            entry.append(heading).append(events.stream().anyMatch(line -> line.startsWith("~ "))
                    ? " (~: by another thread while several tests ran, may belong to another test):\n" : ":\n");
            for (String event : events) {
                entry.append("    ").append(event).append('\n');
            }
        }
        if (mark != null && mark.written()) {
            entry.append(mark.overlapped ? "  logs (other tests ran at the same time: the lines of the test's thread, and of other threads (~) which may be its own): " : "  logs: ")
                    .append(command(mark)).append('\n');
        }
        if (parent != null && parent.written()) {
            entry.append(parent.overlapped ? "  class logs (set-up; other classes ran at the same time: the lines of the class's thread): "
                    : "  class logs (set-up, Spring context start): ").append(command(parent)).append('\n');
        }

        synchronized (totals) {
            if (!totals.headerWritten) {
                totals.headerWritten = true;
                entry.insert(0, "---- JVM pid " + pid + header() + " ----\n");
            }
            append(summaryFile, totals, entry.toString());
        }
    }

    private static void describe(StringBuilder entry, String prefix, Throwable throwable) {
        String message = AiText.singleLine(throwable.getMessage());
        if (message.length() > MAX_MESSAGE) {
            message = AiText.cut(message, MAX_MESSAGE) + "...[truncated, " + message.length() + " chars]";
        }
        entry.append("  ").append(prefix).append(throwable.getClass().getName());
        if (!message.isEmpty()) {
            entry.append(": ").append(message);
        }
        entry.append('\n');
        String frame = frame(throwable);
        if (frame != null) {
            entry.append("  at ").append(frame).append('\n');
        }
    }

    /** The first frame which is not in a framework or the JDK, else the first frame. */
    private static String frame(Throwable throwable) {
        StackTraceElement[] frames = throwable.getStackTrace();
        for (StackTraceElement frame : frames) {
            if (!AiTestEvents.isFramework(frame.getClassName())) {
                return frame.toString();
            }
        }
        return frames.length == 0 ? null : frames[0].toString();
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable root = throwable;
        // bounded, as a cause chain can be circular
        for (int i = 0; i < 100 && root != null && root.getCause() != null && root.getCause() != root; i++) {
            root = root.getCause();
        }
        return root;
    }

    /** Where this JVM runs and, if logging goes to the AI files, its compact file. */
    private String header() {
        String header = " in " + System.getProperty("user.dir");
        return aiFilesWritten.getAsBoolean() ? header + ", logs " + compactFile : header;
    }

    /**
     * Prints the lines between the markers, which are the events of the test, whatever thread logged them. If other tests ran at the
     * same time those lines are not only its own: then the markers and the events logged on the thread of the test.
     */
    private String command(Mark mark) {
        if (mark.overlapped) {
            return "grep '#" + mark.token() + " ' " + compactFile;
        }
        return "sed -n '/#" + mark.token() + " start/,/#" + mark.token() + " end/p' " + compactFile;
    }

    /** The mark of the nearest enclosing test class. */
    private Mark parentMark(TestIdentifier identifier) {
        TestPlan plan = testPlan;
        Optional<TestIdentifier> parent = plan == null ? Optional.empty() : plan.getParent(identifier);
        while (parent.isPresent()) {
            Mark mark = marks.get(parent.get().getUniqueId());
            if (mark != null) {
                return mark;
            }
            parent = plan.getParent(parent.get());
        }
        return null;
    }

    /** Marks the tests below the identifier as seen and returns how many were not seen before. */
    private int markTestsSeen(TestIdentifier identifier) {
        TestPlan plan = testPlan;
        if (plan == null || identifier.isTest() && !identifier.isContainer()) {
            return 0;
        }
        int count = 0;
        for (TestIdentifier descendant : plan.getDescendants(identifier)) {
            if (descendant.isTest() && seen.add(descendant.getUniqueId())) {
                count++;
            }
        }
        return count;
    }

    private static void append(Path summaryFile, Totals totals, String text) throws IOException {
        synchronized (totals) {
            if (!totals.fileCreated) {
                AiFiles.createOwnerOnly(summaryFile);
                totals.fileCreated = true;
            }
            // one write per entry, as several test JVMs append to the file
            try (FileChannel channel = FileChannel.open(summaryFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            }
        }
    }

    // ---- names

    private static boolean isClass(TestIdentifier identifier) {
        return identifier.isContainer() && identifier.getSource().orElse(null) instanceof ClassSource;
    }

    /**
     * <code>SimpleClassName</code> or <code>SimpleClassName.method()</code>, followed by the display name if it tells more (the
     * invocation of a parameterized test, a dynamic test, <code>@DisplayName</code>).
     */
    static String nameOf(TestIdentifier identifier) {
        TestSource source = identifier.getSource().orElse(null);
        String displayName = AiText.singleLine(identifier.getDisplayName());
        if (displayName.length() > MAX_NAME) {
            displayName = AiText.cut(displayName, MAX_NAME) + "...";
        }
        if (source instanceof ClassSource classSource) {
            String name = simpleName(classSource.getClassName());
            return identifier.isContainer() ? name : name + " [" + displayName + "]";
        }
        if (source instanceof MethodSource methodSource) {
            String name = simpleName(methodSource.getClassName()) + "." + methodSource.getMethodName() + "()";
            return displayName.startsWith(methodSource.getMethodName() + "(") ? name : name + " [" + displayName + "]";
        }
        return displayName;
    }

    private static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
