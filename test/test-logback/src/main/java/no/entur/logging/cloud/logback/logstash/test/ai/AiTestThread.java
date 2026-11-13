package no.entur.logging.cloud.logback.logstash.test.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.DefaultCompositeConsoleOutputLoggingEvent;

/**
 * Which test an event belongs to when tests run at the same time and the lines between the markers of a test (see
 * {@link AiTestMarker}) are not only its own. Set by {@code AiTestExecutionListener}, read when the event is logged (not when it is
 * written: by then the thread may be running another test), and written after the logger:
 * <ul>
 * <li>An event logged on the thread of a test has the token of that test (of the test class during its set-up):
 * <code>WARN Logger #t17 : message</code>.</li>
 * <li>An event logged on another thread (a server handling a request, an executor) while several tests are running has the tokens of
 * all of them, after a <code>~</code>: <code>WARN Logger ~ #t17 #t21 : message</code>. It belongs to one of them (or to none). While
 * only one test is running such an event has no token: it is between the markers of that test.</li>
 * </ul>
 * So <code>grep '#t17 '</code> prints the markers of the test, its own events and the events of other threads which may be its own.
 * <p>
 * The token of a thread is not inherited by threads started during the test: a pooled thread would keep the token of the test which
 * happened to start it.
 */
public final class AiTestThread {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private static final Set<String> RUNNING = ConcurrentHashMap.newKeySet();

    // "~ #t17 #t21" while several tests are running, else null
    private static volatile String running;

    private AiTestThread() {
    }

    /** Sets the test running on this thread and returns the previous one (the test class, usually), to be passed to {@link #restore(String)}. */
    public static String set(String token) {
        String previous = CURRENT.get();
        CURRENT.set(token);
        return previous;
    }

    public static void restore(String previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /** The test running on this thread, or null. */
    public static String current() {
        return CURRENT.get();
    }

    /** A test (not a test class) started. */
    public static void started(String token) {
        RUNNING.add(token);
        update();
    }

    public static void finished(String token) {
        RUNNING.remove(token);
        update();
    }

    /** The tests (not test classes) running now. */
    public static List<String> runningTests() {
        return RUNNING.isEmpty() ? List.of() : List.copyOf(RUNNING);
    }

    public static void reset() {
        RUNNING.clear();
        running = null;
    }

    private static void update() {
        synchronized (RUNNING) {
            running = RUNNING.size() < 2 ? null : RUNNING.stream()
                    .sorted(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()))
                    .map(token -> "#" + token).collect(Collectors.joining(" ", "~ ", ""));
        }
    }

    /**
     * What to write for an event logged on this thread now: <code>#t17</code> for the thread of a test, <code>~ #t17 #t21</code> for
     * another thread while several tests are running, else null.
     */
    public static String tag() {
        String token = CURRENT.get();
        return token != null ? "#" + token : running;
    }

    /** The tag of the event, taken when it was logged (see {@link #tag()}), or null. */
    public static String tagOf(ILoggingEvent event) {
        return event instanceof DefaultCompositeConsoleOutputLoggingEvent e ? e.getAiTest() : null;
    }
}
