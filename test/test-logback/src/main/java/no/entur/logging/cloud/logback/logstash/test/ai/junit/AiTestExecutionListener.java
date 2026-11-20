package no.entur.logging.cloud.logback.logstash.test.ai.junit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
 * <b>Summary file</b> (<code>&lt;file&gt;.summary</code>, shared by all test JVMs): a line with the number of tests per test JVM, the
 * command which prints the log lines of a test (with its token), and an entry per failed test (or test class, if its set-up or
 * tear-down failed) with the exception, the first frame which is not framework code and the WARN/ERROR events logged during the test:
 * <pre>
 * ---- JVM pid 4711 in /work/app, logs /tmp/logs/test.4711.compact: 118 tests, 1 failed, 0 aborted, 2 skipped ----
 * log lines: sed -n '/#tN start/,/#tN end/p' /tmp/logs/test.4711.compact with the #tN of a test, or of its class (class #tN: set-up, Spring context start)
 * FAILED DocumentEndpointTest.returnsNotFound() #t17, class #t12
 *   org.opentest4j.AssertionFailedError: expected: &lt;404&gt; but was: &lt;500&gt;
 *   at no.entur.DocumentEndpointTest.returnsNotFound(DocumentEndpointTest.java:42)
 *   WARN/ERROR logged during the test:
 *     ERROR DocumentService #1a2b3c4d5e6f : Unable to load document 7 | java.io.IOException: timeout | at no.entur.DocumentStore.load(DocumentStore.java:88)
 * </pre>
 * The block of a JVM is written when the JVM exits. With many failed tests it stays short:
 * <ul>
 * <li>First the kinds of WARN/ERROR events which several failed tests have in common (and how many passed tests logged them too, so
 * that the likely cause comes first), numbered; an entry refers to them by number (and the id of its own event) instead of repeating
 * them.</li>
 * <li>Tests which failed the same way have one entry, which names the others: the same exception and message, thrown at the same
 * place outside the test's own class (an assertion fails at another line in each test), and the same kinds of WARN/ERROR events.</li>
 * <li>{@value #MAX_DETAILED} entries, then the other failures, one line each.</li>
 * <li>If a test logged nothing, its entry has the events of the set-up of its class (a Spring context start), except those which a
 * test class that passed also logged during its set-up (framework warnings at every context start); the entry says how many it left
 * out.</li>
 * </ul>
 * Everything which is left out or cut is counted or marked (<code>... and 3 more</code>, <code>...[truncated, 812 chars]</code>).
 * <p>
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
    /** Failures written in detail per test JVM (tests which failed the same way are one failure); the others are one line each. */
    static final int MAX_DETAILED = 5;
    /** Failures listed in one line each per test JVM, after the detailed ones. */
    static final int MAX_LISTED = 30;
    /** Other tests named in the entry of a failure. */
    static final int MAX_NAMED = 10;
    /** Kinds of WARN/ERROR events listed as shared by the failed tests. */
    static final int MAX_SHARED = 5;
    private static final int MAX_LISTED_MESSAGE = 160;

    /** A failed test (or test class), as written to the summary file. */
    private static final class Failure {
        // FAILED Class.method() #t17, class #t12
        String title;
        // Class.method(), the simple name of its class and its token: to name it in the entry of another test which failed the same way
        String name;
        String className;
        String token;
        // #t17 Class.method(): AssertionFailedError: expected ...
        String listed;
        // the exception and its root cause, each with its frame, one line each
        final List<String> exception = new ArrayList<>();
        // the frame of the test method in the stack trace of the exception, and the file of the frame shown for the exception: for the
        // line of a test which failed the same way as another, which the entry of the other names
        StackTraceElement testFrame;
        String shownFile;
        // the WARN/ERROR events logged during the test (see AiTestEvents), and how many were not kept
        String eventsHeading;
        List<AiTestEvents.Line> events = List.of();
        int notListed;
        // the events are those of the set-up of the test's class, as the test logged none
        boolean classEvents;
        // the compact file, if the command of the block prints the lines of the test or of its class
        String compactFile;
        // commands for the lines of a test or class which ran at the same time as others, which that command does not print
        final List<String> commands = new ArrayList<>();
        // the exception, where it was thrown outside the test's class and the kinds of events: failures with the same key are one entry
        String key;
        // kinds of WARN/ERROR events logged during the test, see AiTestEvents
        Map<String, String> eventKinds = Map.of();
    }

    /**
     * What ran in this JVM, per summary file. A test JVM may run several test plans (Maven runs one per test class in a reused fork);
     * they share a block in the summary file, written at JVM exit.
     */
    private static final class Totals {
        int tests;
        int failed;
        int aborted;
        int skipped;
        int notRun;
        int failedContainers;
        String header = "";
        final List<Failure> failures = new ArrayList<>();
        // tests which passed, and how many of them logged each kind of WARN/ERROR event
        int passed;
        final Map<String, int[]> passedWithKind = new HashMap<>();
        // the kinds of WARN/ERROR events logged during the set-up of test classes which passed (the class and all its tests)
        final Set<String> passedClassKinds = new HashSet<>();
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
        // for a class: one of its tests (or nested classes) failed
        volatile boolean failedWithin;

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
                // of a test which passed (those of a failed one were taken above), or of the set-up of a class
                AiTestEvents.Events events = AiTestEvents.of(mark.token(), true);
                if (status == TestExecutionResult.Status.SUCCESSFUL) {
                    synchronized (totals) {
                        if (mark.test) {
                            totals.passed++;
                            for (String kind : events.kinds().keySet()) {
                                totals.passedWithKind.computeIfAbsent(kind, k -> new int[1])[0]++;
                            }
                        } else if (!mark.failedWithin) {
                            totals.passedClassKinds.addAll(events.kinds().keySet());
                        }
                    }
                }
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
        } catch (Exception | LinkageError e) {
            enabled = false;
        }
    }

    /**
     * Writes the block of this JVM to the summary file (at JVM exit): a line with the number of tests and, if tests failed, the kinds of
     * WARN/ERROR events the failed tests have in common, the failed tests (one line each) and the details of the first ones.
     */
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
                StringBuilder block = new StringBuilder("---- JVM pid ").append(ProcessHandle.current().pid()).append(totals.header)
                        .append(": ").append(totals.tests + totals.skipped).append(" tests, ").append(totals.failed).append(" failed, ")
                        .append(totals.aborted).append(" aborted, ").append(totals.skipped).append(" skipped");
                if (totals.notRun > 0) {
                    block.append(", ").append(totals.notRun).append(" not run");
                }
                if (totals.failedContainers > 0) {
                    block.append(", ").append(totals.failedContainers).append(" failed test classes (set-up or tear-down)");
                }
                block.append(" ----\n");
                try {
                    StringBuilder failures = new StringBuilder();
                    describeFailures(failures, totals);
                    block.append(failures);
                } catch (RuntimeException | LinkageError e) {
                    // the failed tests at least
                    for (Failure failure : totals.failures) {
                        block.append(failure.title).append('\n');
                    }
                }
                AiFiles.createOwnerOnly(summaryFile);
                // one write per JVM, as several test JVMs append to the file
                try (FileChannel channel = FileChannel.open(summaryFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND, LinkOption.NOFOLLOW_LINKS)) {
                    ByteBuffer buffer = ByteBuffer.wrap(block.toString().getBytes(StandardCharsets.UTF_8));
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                }
            }
        } catch (Exception | LinkageError e) {
            // nothing to be done at exit
        }
    }

    private static void describeFailures(StringBuilder block, Totals totals) {
        List<Failure> failures = totals.failures;
        if (failures.isEmpty()) {
            return;
        }
        // one command for the log lines of all tests and test classes, with the token of one
        for (Failure failure : failures) {
            if (failure.compactFile != null) {
                block.append("log lines: ").append(command("tN", failure.compactFile))
                        .append(" with the #tN of a test, or of its class (class #tN: set-up, Spring context start)\n");
                break;
            }
        }
        Map<String, Integer> shared = describeSharedEvents(block, totals);

        // tests which failed the same way are one failure, with one entry
        Map<Object, List<Failure>> byKey = new LinkedHashMap<>();
        for (Failure failure : failures) {
            byKey.computeIfAbsent(failure.key != null ? failure.key : failure, key -> new ArrayList<>()).add(failure);
        }
        List<List<Failure>> sameFailures = new ArrayList<>(byKey.values());
        int detailed = Math.min(MAX_DETAILED, sameFailures.size());
        for (List<Failure> same : sameFailures.subList(0, detailed)) {
            describe(block, same, shared, totals);
        }
        if (detailed == sameFailures.size()) {
            return;
        }
        List<List<Failure>> others = sameFailures.subList(detailed, sameFailures.size());
        block.append("Other failed tests (").append(others.stream().mapToInt(List::size).sum()).append("), one line per failure:\n");
        for (int i = 0; i < others.size(); i++) {
            if (i == MAX_LISTED) {
                List<List<Failure>> left = others.subList(i, others.size());
                block.append("  ... and ").append(left.size()).append(" more failures, of ").append(left.stream().mapToInt(List::size).sum())
                        .append(" tests\n");
                break;
            }
            List<Failure> same = others.get(i);
            block.append("  ").append(same.get(0).listed);
            if (same.size() > 1) {
                block.append(", and the same in ").append(others(same, false));
            }
            block.append('\n');
        }
    }

    /** The entry of a failure: its first test, with the others which failed the same way named. */
    private static void describe(StringBuilder block, List<Failure> same, Map<String, Integer> shared, Totals totals) {
        Failure failure = same.get(0);
        block.append(failure.title).append('\n');
        for (String line : failure.exception) {
            block.append("  ").append(line).append('\n');
        }
        describeEvents(block, failure, shared, totals);
        if (same.size() > 1) {
            block.append("  the same failure in ").append(others(same, true)).append('\n');
        }
        for (String command : failure.commands) {
            block.append("  ").append(command).append('\n');
        }
    }

    /**
     * The events of the entry: those of a shared kind by number and id, the others in full. Events of the set-up of the test's class
     * which a test class that passed also logged during its set-up are left out, and counted.
     */
    private static void describeEvents(StringBuilder block, Failure failure, Map<String, Integer> shared, Totals totals) {
        List<String> references = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        int alsoPassed = 0;
        boolean otherThreads = false;
        for (AiTestEvents.Line line : failure.events) {
            if (failure.classEvents && totals.passedClassKinds.contains(line.kind())) {
                alsoPassed += line.count();
                continue;
            }
            otherThreads |= line.text().startsWith("~ ");
            Integer number = shared.get(line.kind());
            if (number != null) {
                references.add("[" + number + "] #" + line.id() + (line.count() > 1 ? " (" + line.count() + " times)" : ""));
            } else {
                lines.add(line.toString());
            }
        }
        String passedNote = alsoPassed + (alsoPassed == 1 ? " event" : " events") + " which test classes that passed also logged during their set-up";
        if (references.isEmpty() && lines.isEmpty() && failure.notListed == 0) {
            if (alsoPassed > 0) {
                block.append("  ").append(failure.eventsHeading).append(": only ").append(passedNote).append('\n');
            }
            return;
        }
        block.append("  ").append(failure.eventsHeading);
        if (otherThreads) {
            block.append(" (~: by another thread while several tests ran, may belong to another test)");
        }
        block.append(':');
        if (!references.isEmpty()) {
            block.append(' ').append(String.join(", ", references)).append(" (above)");
            if (!lines.isEmpty() || failure.notListed > 0 || alsoPassed > 0) {
                block.append(", and");
            }
        }
        block.append('\n');
        for (String line : lines) {
            block.append("    ").append(line).append('\n');
        }
        if (failure.notListed > 0) {
            block.append("    ... and ").append(failure.notListed).append(" more WARN/ERROR events\n");
        }
        if (alsoPassed > 0) {
            block.append("    ... and ").append(passedNote).append('\n');
        }
    }

    /**
     * The other tests of a failure: <code>2 more tests: b() at line 52 #t18, Other.c() at Other.java:40 #t19</code>, without the class
     * if it is that of the first. With lines (in the entry of the first, not in the list of other failures): where each test method is
     * in the stack trace of its exception, unless that is where the first one is, as for the invocations of a parameterized test. The
     * line is in the file of the frame shown for the first test's exception, else the file is named.
     */
    private static String others(List<Failure> same, boolean withLines) {
        Failure first = same.get(0);
        int count = same.size() - 1;
        StringBuilder names = new StringBuilder().append(count).append(count == 1 ? " more test: " : " more tests: ");
        for (int i = 1; i < same.size(); i++) {
            if (i > MAX_NAMED) {
                names.append(", and ").append(same.size() - i).append(" more");
                break;
            }
            Failure other = same.get(i);
            String name = other.name;
            if (other.className != null && other.className.equals(first.className) && name.startsWith(other.className + ".")) {
                name = name.substring(other.className.length() + 1);
            }
            names.append(i > 1 ? ", " : "").append(name);
            StackTraceElement frame = other.testFrame;
            if (withLines && frame != null && !(first.testFrame != null && frame.getFileName().equals(first.testFrame.getFileName())
                    && frame.getLineNumber() == first.testFrame.getLineNumber())) {
                names.append(" at ").append(frame.getFileName().equals(first.shownFile) ? "line " : frame.getFileName() + ":")
                        .append(frame.getLineNumber());
            }
            if (other.token != null) {
                names.append(" #").append(other.token);
            }
        }
        return names.toString();
    }

    /**
     * The kinds of WARN/ERROR events logged during more than one failed test, most specific to the failures first: an event logged
     * during all failed tests and no test which passed is likely to be the cause, one logged during most tests is not. Returns the
     * number of each kind listed.
     */
    private static Map<String, Integer> describeSharedEvents(StringBuilder block, Totals totals) {
        List<Failure> failures = totals.failures;
        if (failures.size() < 2) {
            return Map.of();
        }
        Map<String, Integer> failedWith = new LinkedHashMap<>();
        Map<String, String> lines = new HashMap<>();
        for (Failure failure : failures) {
            for (Map.Entry<String, String> kind : failure.eventKinds.entrySet()) {
                failedWith.merge(kind.getKey(), 1, Integer::sum);
                lines.putIfAbsent(kind.getKey(), kind.getValue());
            }
        }
        record Shared(String kind, String line, int failed, int passed, double score) {
        }
        List<Shared> shared = new ArrayList<>();
        for (Map.Entry<String, Integer> kind : failedWith.entrySet()) {
            int[] passedWith = totals.passedWithKind.get(kind.getKey());
            int passed = passedWith == null ? 0 : passedWith[0];
            double score = (double) kind.getValue() / failures.size() - (totals.passed == 0 ? 0 : (double) passed / totals.passed);
            if (kind.getValue() > 1 && score > 0) {
                shared.add(new Shared(kind.getKey(), lines.get(kind.getKey()), kind.getValue(), passed, score));
            }
        }
        if (shared.isEmpty()) {
            return Map.of();
        }
        shared.sort(Comparator.comparingDouble(Shared::score).reversed().thenComparing(Comparator.comparingInt(Shared::failed).reversed()));
        block.append("WARN/ERROR events logged during several failed tests, likely causes first (one event of each kind):\n");
        Map<String, Integer> numbers = new HashMap<>();
        for (int i = 0; i < shared.size() && i < MAX_SHARED; i++) {
            Shared event = shared.get(i);
            numbers.put(event.kind(), i + 1);
            block.append("  [").append(i + 1).append("] ").append(event.failed()).append(" of ").append(failures.size()).append(" failed, ")
                    .append(event.passed()).append(" of ").append(totals.passed).append(" passed tests: ").append(event.line()).append('\n');
        }
        if (shared.size() > MAX_SHARED) {
            block.append("  ... and ").append(shared.size() - MAX_SHARED).append(" more kinds\n");
        }
        return numbers;
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

    private void writeFailure(TestIdentifier identifier, Mark mark, Throwable throwable, int missing) {
        Failure failure = new Failure();
        String name = mark != null ? mark.name() : nameOf(identifier);
        String testClass = classOf(identifier);
        failure.name = name;
        failure.className = testClass == null ? null : simpleName(testClass);
        failure.token = mark == null ? null : mark.token();
        Mark parent = parentMark(identifier);
        if (parent != null) {
            parent.failedWithin = true;
        }

        String kind = identifier.isTest() ? "" : isClass(identifier) ? " (test class set-up or tear-down)" : " (container)";
        StringBuilder title = new StringBuilder("FAILED ").append(name).append(kind);
        if (mark != null) {
            title.append(" #").append(mark.token());
        }
        if (missing > 0) {
            title.append(", ").append(missing).append(" tests not run");
        }
        // the lines of the test and of its class are printed by the command of the block, unless other tests ran at the same time
        boolean byToken = mark != null && mark.written() && !mark.overlapped;
        if (parent != null && parent.written() && !parent.overlapped) {
            title.append(", class #").append(parent.token());
            byToken = true;
        }
        failure.title = title.toString();
        failure.compactFile = byToken ? compactFile : null;

        Throwable root = rootCause(throwable);
        StringBuilder listed = new StringBuilder(mark != null ? "#" + mark.token() + " " : "").append(name).append(kind);
        String key = null;
        if (throwable != null) {
            // not the frame in the test's own class: the same assertion fails at another line in each test
            key = throwable.getClass().getName() + ':' + throwable.getMessage() + '|' + root.getClass().getName() + ':' + root.getMessage()
                    + '|' + keyFrame(throwable, testClass) + '|' + keyFrame(root, testClass);
            String singleLine = AiText.singleLine(root.getMessage());
            String message = AiText.squeezeIfLonger(singleLine, MAX_LISTED_MESSAGE);
            listed.append(": ").append(simpleName(root.getClass().getName()));
            if (!message.isEmpty()) {
                listed.append(": ").append(message.length() > MAX_LISTED_MESSAGE
                        ? AiText.cut(message, MAX_LISTED_MESSAGE) + "...[truncated, " + singleLine.length() + " chars]" : message);
            }
            StringBuilder description = new StringBuilder();
            describe(description, "", throwable);
            if (root != throwable) {
                describe(description, "root cause: ", root);
            }
            description.toString().lines().forEach(line -> failure.exception.add(line.strip()));
            failure.testFrame = testFrame(identifier, throwable, root);
            StackTraceElement shown = AiTestEvents.frameOf(throwable.getStackTrace());
            failure.shownFile = shown == null ? null : shown.getFileName();
        }
        failure.listed = listed.toString();

        // what the test logged at WARN and ERROR, so that the cause can be seen without running a command
        AiTestEvents.Events events = mark == null ? AiTestEvents.of("", false) : AiTestEvents.of(mark.token(), true);
        failure.eventKinds = events.kinds();
        failure.eventsHeading = identifier.isTest() ? "WARN/ERROR logged during the test" : "WARN/ERROR logged during the set-up or tear-down";
        if (events.isEmpty() && parent != null) {
            // for example a Spring context which failed to start: logged before the test started
            events = AiTestEvents.of(parent.token(), false);
            failure.eventsHeading = "WARN/ERROR logged during the set-up of the test class";
            failure.classEvents = true;
        }
        failure.events = events.lines();
        failure.notListed = events.notListed();
        // tests which failed the same way: also the same kinds of events (of the test, or of its class if it logged none), so that
        // tests which fail alike for different reasons are not one
        failure.key = key == null ? null : key + '|' + failure.classEvents + '|' + String.join("|", new TreeSet<>(events.kinds().keySet()));

        if (mark != null && mark.written() && mark.overlapped) {
            failure.commands.add("logs (other tests ran at the same time: the lines of the test's thread, and of other threads (~) which may be its own): "
                    + command(mark));
        }
        if (parent != null && parent.written() && parent.overlapped) {
            failure.commands.add("class logs (set-up; other classes ran at the same time: the lines of the class's thread): " + command(parent));
        }
        synchronized (totals) {
            totals.failures.add(failure);
        }
    }

    /** The frame of an exception for the key of a failure: none if it is in the test's own class (or a class nested in it). */
    private static String keyFrame(Throwable throwable, String testClass) {
        StackTraceElement frame = AiTestEvents.frameOf(throwable.getStackTrace());
        if (frame == null || testClass != null && outerClass(frame.getClassName()).equals(outerClass(testClass))) {
            return "";
        }
        return frame.toString();
    }

    /**
     * The frame of the test method in the stack trace of the exception, else of its root cause: the line of the test where it failed, or
     * where it called what failed. Null for a test class, or if neither has a frame of the method with a line.
     */
    private static StackTraceElement testFrame(TestIdentifier identifier, Throwable throwable, Throwable root) {
        if (!(identifier.getSource().orElse(null) instanceof MethodSource source)) {
            return null;
        }
        for (Throwable exception : List.of(throwable, root)) {
            StackTraceElement[] frames = exception.getStackTrace();
            for (StackTraceElement frame : frames == null ? new StackTraceElement[0] : frames) {
                if (frame.getClassName().equals(source.getClassName()) && frame.getMethodName().equals(source.getMethodName())
                        && frame.getFileName() != null && frame.getLineNumber() > 0) {
                    return frame;
                }
            }
        }
        return null;
    }

    private static String outerClass(String className) {
        int nested = className.indexOf('$');
        return nested == -1 ? className : className.substring(0, nested);
    }

    /** The class of a test or test class, null if it has none (a dynamic container, for example). */
    private static String classOf(TestIdentifier identifier) {
        TestSource source = identifier.getSource().orElse(null);
        if (source instanceof MethodSource methodSource) {
            return methodSource.getClassName();
        }
        return source instanceof ClassSource classSource ? classSource.getClassName() : null;
    }

    private static void describe(StringBuilder entry, String prefix, Throwable throwable) {
        String singleLine = AiText.singleLine(throwable.getMessage());
        String message = AiText.squeezeIfLonger(singleLine, MAX_MESSAGE);
        if (message.length() > MAX_MESSAGE) {
            message = AiText.cut(message, MAX_MESSAGE) + "...[truncated, " + singleLine.length() + " chars]";
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

    /** The first frame in the project's own code, else the first one outside frameworks and the JDK, see {@link AiTestEvents#frameOf}. */
    private static String frame(Throwable throwable) {
        StackTraceElement frame = AiTestEvents.frameOf(throwable.getStackTrace());
        return frame == null ? null : frame.toString();
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
        return command(mark.token(), compactFile);
    }

    private static String command(String token, String file) {
        return "sed -n '/#" + token + " start/,/#" + token + " end/p' " + file;
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
