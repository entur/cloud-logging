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
 * event is logged, so also events which the compact file collapses as repeated. Only a few lines per test are kept, repeats are counted,
 * and the kinds of events of a test are kept to tell which ones the failed tests have in common.
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

    /** Kinds of events kept per test to tell which events the failed tests have in common. */
    static final int MAX_KINDS = 50;

    /** The WARN and ERROR events of a test. */
    public static final class Events {
        // by logger and message, in order
        final Map<String, int[]> counts = new LinkedHashMap<>();
        final Map<String, String> lines = new LinkedHashMap<>();
        // by kind of event (see kind()), with a line describing one of them
        final Map<String, String> kinds = new LinkedHashMap<>();
        int total;

        public synchronized boolean isEmpty() {
            return total == 0;
        }

        /** One line per event, and a last line if there were more. */
        public synchronized List<String> lines() {
            List<String> result = new ArrayList<>();
            int shown = 0;
            for (Map.Entry<String, String> line : lines.entrySet()) {
                int count = counts.get(line.getKey())[0];
                shown += count;
                result.add(count > 1 ? line.getValue() + " (" + count + " times)" : line.getValue());
            }
            if (total > shown) {
                result.add("... and " + (total - shown) + " more WARN/ERROR events");
            }
            return result;
        }

        /**
         * The kinds of events: events are of the same kind if they have the same logger and were thrown at the same place (root cause
         * and its first frame outside frameworks) or, without an exception, have the same message apart from digits. With a line
         * describing one event of the kind.
         */
        public synchronized Map<String, String> kinds() {
            return new LinkedHashMap<>(kinds);
        }
    }

    private static final Events NONE = new Events();

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

    // whether a class is the project's own code, by class name
    private static final Map<String, Boolean> PROJECT_CODE = new ConcurrentHashMap<>();

    /**
     * True if the class is the project's own code: loaded from a directory of class files (<code>target/classes</code>,
     * <code>build/classes/java/main</code>, ...) and not from a jar. False if that cannot be told.
     */
    static boolean isProjectCode(String className) {
        if (PROJECT_CODE.size() > 10_000) {
            PROJECT_CODE.clear();
        }
        return PROJECT_CODE.computeIfAbsent(className, name -> {
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> type = Class.forName(name, false, loader != null ? loader : AiTestEvents.class.getClassLoader());
                java.security.CodeSource source = type.getProtectionDomain().getCodeSource();
                return source != null && source.getLocation() != null && source.getLocation().getPath().endsWith("/");
            } catch (Throwable e) {
                return false;
            }
        });
    }

    /**
     * The frame which tells most about where in the project something happened: the first one in the project's own code, else the
     * first one outside test frameworks, assertion libraries and the JDK (an exception thrown within a library), else the first one.
     * Null if there are none.
     */
    public static StackTraceElement frameOf(StackTraceElement[] frames) {
        if (frames == null || frames.length == 0) {
            return null;
        }
        StackTraceElement outsideFrameworks = null;
        for (StackTraceElement frame : frames) {
            if (isFramework(frame.getClassName())) {
                continue;
            }
            if (isProjectCode(frame.getClassName())) {
                return frame;
            }
            if (outsideFrameworks == null) {
                outsideFrameworks = frame;
            }
        }
        return outsideFrameworks != null ? outsideFrameworks : frames[0];
    }

    /**
     * The frame of the class which logged the event (its logger is named after it), or of a class nested in it: where that class
     * called what failed. An exception which is caught and logged is often thrown further down, in code which only got a wrong
     * value, and this is the frame nearest to the log statement. Null if the class is not in the stack trace.
     */
    public static StackTraceElement callerFrameOf(StackTraceElement[] frames, String loggerName) {
        if (frames == null || loggerName == null) {
            return null;
        }
        for (StackTraceElement frame : frames) {
            String className = frame.getClassName();
            if (className.equals(loggerName) || className.startsWith(loggerName) && className.charAt(loggerName.length()) == '$') {
                return frame;
            }
        }
        return null;
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
        String kind = kind(event);
        String line = null;
        for (String owner : owners) {
            Events events = EVENTS.computeIfAbsent(owner, token -> new Events());
            synchronized (events) {
                events.total++;
                boolean newKind = !events.kinds.containsKey(kind) && events.kinds.size() < MAX_KINDS;
                int[] count = events.counts.get(key);
                boolean newLine = count == null && events.counts.size() < MAX_EVENTS;
                if (count != null) {
                    count[0]++;
                }
                if (newKind || newLine) {
                    if (line == null) {
                        line = (own == null && owners.size() > 1 ? "~ " : "") + describe(event);
                    }
                    if (newLine) {
                        events.counts.put(key, new int[] { 1 });
                        events.lines.put(key, line);
                    }
                    if (newKind) {
                        events.kinds.put(kind, line);
                    }
                }
            }
        }
    }

    /** The events of the test (or test class); they are forgotten if <code>remove</code>. */
    public static Events of(String token, boolean remove) {
        Events events = remove ? EVENTS.remove(token) : EVENTS.get(token);
        return events == null ? NONE : events;
    }

    public static void reset() {
        EVENTS.clear();
    }

    private static String kind(ILoggingEvent event) {
        IThrowableProxy root = rootCause(event.getThrowableProxy());
        if (root == null) {
            return event.getLevel() + "|" + event.getLoggerName() + '|' + withoutDigits(String.valueOf(event.getFormattedMessage()));
        }
        return event.getLevel() + "|" + event.getLoggerName() + '|' + root.getClassName() + '|' + frames(root, event.getLoggerName());
    }

    private static String withoutDigits(String text) {
        StringBuilder builder = new StringBuilder(Math.min(text.length(), 200));
        for (int i = 0; i < text.length() && builder.length() < 200; i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                builder.append(c);
            } else if (builder.length() == 0 || builder.charAt(builder.length() - 1) != '#') {
                builder.append('#');
            }
        }
        return builder.toString();
    }

    private static IThrowableProxy rootCause(IThrowableProxy root) {
        // bounded, as a cause chain can be circular
        for (int i = 0; i < 100 && root != null && root.getCause() != null && root.getCause() != root; i++) {
            root = root.getCause();
        }
        return root;
    }

    /**
     * <code> | at frame</code> (see {@link #frameOf(StackTraceElement[])}), followed by <code> | called from frame</code> if the class which
     * logged the event is further down in the stack trace (see {@link #callerFrameOf(StackTraceElement[], String)}); empty if the
     * exception has no frames.
     */
    private static String frames(IThrowableProxy throwable, String loggerName) {
        StackTraceElementProxy[] proxies = throwable.getStackTraceElementProxyArray();
        if (proxies == null || proxies.length == 0) {
            return "";
        }
        StackTraceElement[] frames = new StackTraceElement[proxies.length];
        for (int i = 0; i < proxies.length; i++) {
            frames[i] = proxies[i].getStackTraceElement();
        }
        StackTraceElement thrown = frameOf(frames);
        StackTraceElement caller = callerFrameOf(frames, loggerName);
        String result = " | at " + thrown;
        return caller == null || caller.equals(thrown) ? result : result + " | called from " + caller;
    }

    /** <code>WARN Logger #id : message | root cause: message | at frame</code>, on one line. */
    private static String describe(ILoggingEvent event) {
        String logger = event.getLoggerName();
        StringBuilder line = new StringBuilder().append(event.getLevel()).append(' ').append(logger.substring(logger.lastIndexOf('.') + 1))
                .append(" #").append(AiEventId.idOf(event)).append(" : ").append(cut(event.getFormattedMessage(), MAX_MESSAGE));
        IThrowableProxy root = rootCause(event.getThrowableProxy());
        if (root != null) {
            line.append(" | ").append(root.getClassName());
            if (root.getMessage() != null && !root.getMessage().isEmpty()) {
                line.append(": ").append(cut(root.getMessage(), MAX_EXCEPTION_MESSAGE));
            }
            line.append(frames(root, event.getLoggerName()));
        }
        return line.toString();
    }

    private static String cut(String text, int max) {
        String singleLine = AiText.singleLine(text);
        return singleLine.length() > max ? AiText.cut(singleLine, max) + "..." : singleLine;
    }
}
