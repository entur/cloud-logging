package no.entur.logging.cloud.logback.logstash.test.ai;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The tests of a test run and the WARN/ERROR events logged while they ran, for the failures file: per failed test, the events logged
 * while it ran. Tests are reported by {@link AiTestExecutionListener}, events by {@link FailuresFileAppender}. One instance per JVM
 * ({@link #current()}), so that it survives logback being reconfigured (for example by Spring Boot for every application context).
 * <p>
 * An event belongs to a test for certain if it was logged on the test's thread while the test ran, and not to it if it was logged on the
 * thread of another test (or test class) while that ran. Events logged on other threads
 * (thread pools, servers) are matched by time, from the start of the test's class (so that <code>@BeforeAll</code> and the start of a Spring
 * context count, but not what other tests logged before) to the end of the test; when other tests ran at the same time, they may belong
 * to those instead, and the report says so.
 */
public final class AiTestRun {

    /** A test or a container (such as a test class) as the test engine reported it. */
    record Test(String id, String parentId, String name, boolean isTest, boolean isClass, String thread, Instant start, Instant end,
                boolean failed, String failure) {
    }

    /** A WARN/ERROR event, rendered as in the compact file (with its <code>#id</code>). */
    record Event(Instant time, String thread, String logger, String level, String line) {
    }

    static final int MAX_EVENTS = 10_000;

    static final int MAX_EVENTS_PER_TEST = 10;

    static final int MAX_LINE_LENGTH = 500;

    private static final java.util.regex.Pattern EVENT_ID = java.util.regex.Pattern.compile(" #[0-9a-f]{12}");

    private static final AiTestRun CURRENT = new AiTestRun();

    private final Map<String, Test> tests = new LinkedHashMap<>();
    private final Deque<Event> events = new ArrayDeque<>();
    private long droppedEvents;

    public static AiTestRun current() {
        return CURRENT;
    }

    public synchronized void testStarted(String id, String parentId, String name, boolean isTest, boolean isClass, String thread, Instant time) {
        tests.put(id, new Test(id, parentId, name, isTest, isClass, thread, time, null, false, null));
    }

    public synchronized void testFinished(String id, Instant time, boolean failed, String failure) {
        Test test = tests.get(id);
        if (test != null) {
            tests.put(id, new Test(test.id(), test.parentId(), test.name(), test.isTest(), test.isClass(), test.thread(), test.start(), time, failed,
                    failure));
        }
    }

    /** Keeps the event; the oldest are dropped when more than {@value #MAX_EVENTS} are kept. */
    public synchronized void event(Instant time, String thread, String logger, String level, String line) {
        if (events.size() >= MAX_EVENTS) {
            events.removeFirst();
            droppedEvents++;
        }
        events.addLast(new Event(time, thread, logger, level, line));
    }

    /** Forgets the tests and events reported so far (after the report is written). */
    public synchronized void clear() {
        tests.clear();
        events.clear();
        droppedEvents = 0;
    }

    /** The report: per failed test its failure and the events logged while it ran, or, if no test failed, the events per logger. */
    public synchronized String report() {
        List<Test> failed = tests.values().stream().filter(t -> t.failed() && t.end() != null).toList();
        StringBuilder b = new StringBuilder();
        if (failed.isEmpty()) {
            b.append("No tests failed. ");
            appendEventsPerLogger(b);
            return b.toString();
        }
        b.append(failed.size()).append(failed.size() == 1 ? " test" : " tests")
                .append(" failed. WARN/ERROR events logged while they ran (#id finds the whole event in the full file):\n");
        if (droppedEvents > 0) {
            b.append("(the first ").append(droppedEvents).append(" events of the run are not kept, only the last ").append(MAX_EVENTS).append(")\n");
        }
        List<String> previous = null;
        for (Test test : failed) {
            b.append('\n').append(test.name());
            if (test.failure() != null) {
                b.append(": ").append(test.failure());
            }
            b.append('\n');
            List<String> lines = eventLines(test);
            if (lines.equals(previous) && !lines.isEmpty()) {
                b.append("  (the same events as the test above)\n");
            } else {
                lines.forEach(line -> b.append(line).append('\n'));
            }
            previous = lines;
        }
        return b.toString();
    }

    private List<String> eventLines(Test test) {
        Instant from = windowStart(test);
        List<Event> own = new ArrayList<>();
        List<Event> other = new ArrayList<>();
        for (Event event : events) {
            if (event.time().isBefore(from) || event.time().isAfter(test.end())) {
                continue;
            }
            // before the test started, only set-up counts (@BeforeAll, a Spring context starting), not what other tests logged
            if (event.time().isBefore(test.start()) && duringOtherTest(event, test)) {
                continue;
            }
            boolean onTestThread = Objects.equals(event.thread(), test.thread()) && !event.time().isBefore(test.start());
            if (!onTestThread && onThreadOfOtherTest(event, test)) {
                // logged on the thread of another test (or class) while it ran: that one's for certain
                continue;
            }
            (onTestThread ? own : other).add(event);
        }
        List<String> lines = new ArrayList<>();
        if (own.isEmpty() && other.isEmpty()) {
            lines.add("  (no WARN/ERROR events while this test ran; look in the compact file)");
            return lines;
        }
        List<String> ownLines = collapsed(own);
        List<String> otherLines = collapsed(other);
        int shown = 0;
        for (String line : ownLines) {
            if (shown++ < MAX_EVENTS_PER_TEST) {
                lines.add(line);
            }
        }
        long concurrent = concurrentTests(test);
        if (!otherLines.isEmpty() && concurrent > 0 && shown < MAX_EVENTS_PER_TEST) {
            lines.add("  logged on threads of no test while " + concurrent + (concurrent == 1 ? " other test" : " other tests")
                    + " ran at the same time, so they may belong to those:");
        }
        for (String line : otherLines) {
            if (shown++ < MAX_EVENTS_PER_TEST) {
                lines.add(line);
            }
        }
        if (shown > MAX_EVENTS_PER_TEST) {
            lines.add("  ... and " + (shown - MAX_EVENTS_PER_TEST) + " more WARN/ERROR events while this test ran");
        }
        return lines;
    }

    /** One line per distinct event (ignoring the #id), the first one's, with the number of times if more than once. */
    private static List<String> collapsed(List<Event> events) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Event> first = new LinkedHashMap<>();
        for (Event event : events) {
            String key = EVENT_ID.matcher(event.line()).replaceFirst("");
            counts.merge(key, 1, Integer::sum);
            first.putIfAbsent(key, event);
        }
        List<String> lines = new ArrayList<>();
        first.forEach((key, event) -> {
            int count = counts.get(key);
            lines.add(count == 1 ? line(event) : line(event) + " (" + count + " times)");
        });
        return lines;
    }

    private static String line(Event event) {
        String line = event.line();
        if (line.length() > MAX_LINE_LENGTH) {
            line = line.substring(0, MAX_LINE_LENGTH) + "...";
        }
        return "  " + line;
    }

    /** The start of the test's class (or other enclosing container that is a class), so that its set-up counts; else the test's own start. */
    private Instant windowStart(Test test) {
        Instant start = test.start();
        Test parent = test.parentId() == null ? null : tests.get(test.parentId());
        while (parent != null) {
            if (parent.isClass() && parent.start().isBefore(start)) {
                start = parent.start();
            }
            parent = parent.parentId() == null ? null : tests.get(parent.parentId());
        }
        return start;
    }

    /** Whether the event was logged on the thread of another test or container (not the test's own class) while that ran. */
    private boolean onThreadOfOtherTest(Event event, Test test) {
        for (Test other : tests.values()) {
            if (other.id().equals(test.id()) || test.id().startsWith(other.id()) || !Objects.equals(other.thread(), event.thread())) {
                continue;
            }
            if (!event.time().isBefore(other.start()) && (other.end() == null || !event.time().isAfter(other.end()))) {
                return true;
            }
        }
        return false;
    }

    private boolean duringOtherTest(Event event, Test test) {
        return tests.values().stream()
                .anyMatch(t -> t.isTest() && !t.id().equals(test.id()) && !event.time().isBefore(t.start())
                        && (t.end() == null || !event.time().isAfter(t.end())));
    }

    /** The number of other tests (not containers) which ran at the same time as the given one. */
    private long concurrentTests(Test test) {
        return tests.values().stream()
                .filter(t -> t.isTest() && !t.id().equals(test.id()) && !t.id().startsWith(test.id()) && !test.id().startsWith(t.id()))
                .filter(t -> t.start().isBefore(test.end()) && (t.end() == null || t.end().isAfter(test.start())))
                .count();
    }

    private void appendEventsPerLogger(StringBuilder b) {
        if (events.isEmpty()) {
            b.append("No WARN/ERROR events.\n");
            return;
        }
        Map<String, Integer> counts = new TreeMap<>();
        for (Event event : events) {
            counts.merge(event.level() + " " + event.logger(), 1, Integer::sum);
        }
        b.append("WARN/ERROR events per logger:\n");
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> b.append(String.format("%7d %s%n", e.getValue(), e.getKey())));
    }
}
