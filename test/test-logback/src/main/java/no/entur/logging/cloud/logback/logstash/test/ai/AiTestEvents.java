package no.entur.logging.cloud.logback.logstash.test.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiEventId;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiText;

/**
 * The WARN and ERROR events of the running tests, kept so that the summary file can show what a failed test logged without a command
 * having to be run (see {@code AiTestExecutionListener}). An event belongs to the test running on its thread; an event of another
 * thread (a server, an executor) to all tests running then, marked with <code>~</code> if there is more than one. Collected when the
 * event is logged, so also events which the compact file collapses as repeated. Only a few lines per test are kept, repeats are counted.
 */
public final class AiTestEvents {

    /** Distinct events kept per test. */
    static final int MAX_EVENTS = 8;
    private static final int MAX_MESSAGE = 300;
    private static final int MAX_EXCEPTION_MESSAGE = 200;

    /** Frames of test frameworks, assertion libraries and the JDK, which do not tell where in the project something happened. */
    private static final List<String> FRAMEWORK_PACKAGES = List.of("java.", "javax.", "jdk.", "sun.", "com.sun.", "kotlin.", "kotlinx.",
            "org.junit.", "org.opentest4j.", "org.assertj.", "org.hamcrest.", "org.mockito.", "com.google.common.truth.",
            "org.springframework.", "org.gradle.", "worker.org.gradle.", "org.apache.maven.");

    private static final class Events {
        // by logger and message, in order
        final Map<String, int[]> counts = new LinkedHashMap<>();
        final Map<String, String> lines = new LinkedHashMap<>();
        int total;
    }

    private static final Map<String, Events> EVENTS = new ConcurrentHashMap<>();

    private static volatile boolean enabled;

    private AiTestEvents() {
    }

    /** Switched on by the listener: without it nothing is collected. */
    public static void enable() {
        enabled = true;
    }

    public static boolean isFramework(String className) {
        for (String prefix : FRAMEWORK_PACKAGES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Called on the thread which logs the event. */
    public static void logged(ILoggingEvent event) {
        if (!enabled || event.getLevel() == null || !event.getLevel().isGreaterOrEqual(Level.WARN)) {
            return;
        }
        String own = AiTestThread.current();
        List<String> owners = own != null ? List.of(own) : AiTestThread.runningTests();
        if (owners.isEmpty()) {
            return;
        }
        String key = event.getLoggerName() + '|' + event.getFormattedMessage();
        String line = null;
        for (String owner : owners) {
            Events events = EVENTS.computeIfAbsent(owner, token -> new Events());
            synchronized (events) {
                events.total++;
                int[] count = events.counts.get(key);
                if (count != null) {
                    count[0]++;
                } else if (events.counts.size() < MAX_EVENTS) {
                    if (line == null) {
                        line = (own == null && owners.size() > 1 ? "~ " : "") + describe(event);
                    }
                    events.counts.put(key, new int[] { 1 });
                    events.lines.put(key, line);
                }
            }
        }
    }

    /**
     * The events of the test (or test class), one line each, and a last line if there were more; empty if it logged no WARN or ERROR.
     * The events are forgotten if <code>remove</code>.
     */
    public static List<String> of(String token, boolean remove) {
        Events events = remove ? EVENTS.remove(token) : EVENTS.get(token);
        if (events == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        synchronized (events) {
            int shown = 0;
            for (Map.Entry<String, String> line : events.lines.entrySet()) {
                int count = events.counts.get(line.getKey())[0];
                shown += count;
                result.add(count > 1 ? line.getValue() + " (" + count + " times)" : line.getValue());
            }
            if (events.total > shown) {
                result.add("... and " + (events.total - shown) + " more WARN/ERROR events");
            }
        }
        return result;
    }

    public static void reset() {
        EVENTS.clear();
    }

    /** <code>WARN Logger #id : message | root cause: message | at frame</code>, on one line. */
    private static String describe(ILoggingEvent event) {
        String logger = event.getLoggerName();
        StringBuilder line = new StringBuilder().append(event.getLevel()).append(' ').append(logger.substring(logger.lastIndexOf('.') + 1))
                .append(" #").append(AiEventId.idOf(event)).append(" : ").append(cut(event.getFormattedMessage(), MAX_MESSAGE));
        IThrowableProxy root = event.getThrowableProxy();
        // bounded, as a cause chain can be circular
        for (int i = 0; i < 100 && root != null && root.getCause() != null && root.getCause() != root; i++) {
            root = root.getCause();
        }
        if (root != null) {
            line.append(" | ").append(root.getClassName());
            if (root.getMessage() != null && !root.getMessage().isEmpty()) {
                line.append(": ").append(cut(root.getMessage(), MAX_EXCEPTION_MESSAGE));
            }
            StackTraceElementProxy[] frames = root.getStackTraceElementProxyArray();
            if (frames != null) {
                for (StackTraceElementProxy frame : frames) {
                    if (!isFramework(frame.getStackTraceElement().getClassName())) {
                        line.append(" | at ").append(frame.getStackTraceElement());
                        break;
                    }
                }
            }
        }
        return line.toString();
    }

    private static String cut(String text, int max) {
        String singleLine = AiText.singleLine(text);
        return singleLine.length() > max ? AiText.cut(singleLine, max) + "..." : singleLine;
    }
}
