package no.entur.logging.cloud.logback.logstash.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI mode: compact test logging (written to file) meant for AI tooling.
 */
public class AiLoggingControl {

    /** JVM system property; if 'true', test logging is written as compact output meant for AI tooling. */
    public static final String AI_ENABLED_PROPERTY = "entur.logging.ai.enabled";

    /** Exactly <code>true</code>, as for the condition in <code>logback-test.xml</code> (which is case-sensitive). */
    public static boolean isAiEnabled() {
        return "true".equals(System.getProperty(AI_ENABLED_PROPERTY));
    }

    private static volatile boolean aiFilesStarted;

    /**
     * True if AI mode is enabled and the AI log files are written, i.e. the starter's <code>logback-test.xml</code> included
     * <code>logback-ai.xml</code> (a project's own <code>logback-test.xml</code> may shadow it). Request-response logging only switches
     * to the AI sinks then, so that the human-readable console output is kept otherwise.
     */
    public static boolean isAiActive() {
        return aiFilesStarted && isAiEnabled();
    }

    static void aiFileStarted() {
        aiFilesStarted = true;
    }

    /** JVM system property; max number of characters of a request/response body in compact output. Default 4096; 0 means unlimited. */
    public static final String AI_BODY_MAX_PROPERTY = "entur.logging.ai.request-response.max-body-size";

    public static final int DEFAULT_BODY_MAX = 4096;

    /** Separates the body from the rest of a compact request-response message. */
    public static final String BODY_MARKER = " | body: ";

    public static int bodyMax() {
        return Integer.getInteger(AI_BODY_MAX_PROPERTY, DEFAULT_BODY_MAX);
    }

    public static String truncateBody(String body) {
        return truncate(body, bodyMax());
    }

    /** JVM system property; max number of characters of a message (and of an exception message) in compact output. Default 4096; 0 means unlimited. */
    public static final String AI_MESSAGE_MAX_PROPERTY = "entur.logging.ai.message.max-size";

    public static final int DEFAULT_MESSAGE_MAX = 4096;

    public static int messageMax() {
        return Integer.getInteger(AI_MESSAGE_MAX_PROPERTY, DEFAULT_MESSAGE_MAX);
    }

    public static String truncateMessage(String message) {
        return truncate(message, messageMax());
    }

    /** Cut to <code>max</code> characters (not inside an alias), followed by <code>...[truncated, N chars]</code>. */
    static String truncate(String text, int max) {
        if (max > 0 && text.length() > max) {
            return AiText.cut(text, max) + "...[truncated, " + text.length() + " chars]";
        }
        return text;
    }

    /**
     * JVM system property; comma-separated request-response header names left out of the compact file (case-insensitive).
     * Replaces the default list; set to an empty value to keep all headers.
     */
    public static final String AI_EXCLUDED_HEADERS_PROPERTY = "entur.logging.ai.request-response.excluded-headers";

    /** Lower-case header names to leave out of the compact file: the property {@link #AI_EXCLUDED_HEADERS_PROPERTY} if set, otherwise the given defaults. */
    public static Set<String> excludedHeaders(Set<String> defaults) {
        String value = System.getProperty(AI_EXCLUDED_HEADERS_PROPERTY);
        if (value == null) {
            return defaults;
        }
        return split(value).stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    /** JVM system property; max number of frames kept per exception of a stack trace in compact output. Default 2; 0 means unlimited. */
    public static final String AI_STACKTRACE_MAX_LINES_PROPERTY = "entur.logging.ai.stacktrace.max-lines";

    public static final int DEFAULT_STACKTRACE_MAX_LINES = 2;

    /** JVM system property; max number of exceptions (root cause first) kept of a stack trace in compact output. Unlimited if not set (or 0). */
    public static final String AI_STACKTRACE_MAX_CAUSES_PROPERTY = "entur.logging.ai.stacktrace.max-causes";

    public static int stackTraceMaxLines() {
        return Integer.getInteger(AI_STACKTRACE_MAX_LINES_PROPERTY, DEFAULT_STACKTRACE_MAX_LINES);
    }

    public static int stackTraceMaxCauses() {
        return Integer.getInteger(AI_STACKTRACE_MAX_CAUSES_PROPERTY, 0);
    }

    /** JVM system property; set to <code>false</code> to disable collapsing of repeated WARN/ERROR events in the compact file. Default true. */
    public static final String AI_DEDUPE_PROPERTY = "entur.logging.ai.dedupe";

    public static boolean isDedupeEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(AI_DEDUPE_PROPERTY));
    }

    /**
     * JVM system property; set to <code>false</code> to keep UUIDs (in MDC values and messages) and hex ids (in MDC values) instead of
     * replacing them with short aliases (u1, h1, ...). Default true.
     */
    public static final String AI_ALIAS_PROPERTY = "entur.logging.ai.alias";

    private static volatile boolean aliasesDisabledInThisJvm;

    public static boolean isAliasEnabled() {
        return !aliasesDisabledInThisJvm && !"false".equalsIgnoreCase(System.getProperty(AI_ALIAS_PROPERTY));
    }

    /** Called if another test JVM writes to the same files at the same time, as its aliases would be ambiguous (see {@link AiFileAppender}). */
    public static void disableAliasesInThisJvm() {
        aliasesDisabledInThisJvm = true;
    }

    /**
     * JVM system property; comma-separated logger name prefixes whose events below WARN are left out of the compact file
     * (framework start-up and shutdown chatter). Replaces the default list; set to an empty value to keep everything.
     */
    public static final String AI_QUIET_LOGGERS_PROPERTY = "entur.logging.ai.quiet-loggers";

    public static List<String> quietLoggers(List<String> defaults) {
        String value = System.getProperty(AI_QUIET_LOGGERS_PROPERTY);
        if (value == null) {
            return defaults;
        }
        return split(value);
    }

    private static List<String> split(String value) {
        if (value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableList());
    }
}
