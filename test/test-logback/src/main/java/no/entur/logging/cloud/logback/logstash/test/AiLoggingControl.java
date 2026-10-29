package no.entur.logging.cloud.logback.logstash.test;

/**
 * AI mode: compact test logging (written to file) meant for AI tooling.
 */
public class AiLoggingControl {

    /** JVM system property; if 'true', test logging is written as compact output meant for AI tooling. */
    public static final String AI_ENABLED_PROPERTY = "entur.logging.ai.enabled";

    public static boolean isAiEnabled() {
        return Boolean.getBoolean(AI_ENABLED_PROPERTY);
    }

    /** JVM system property; max number of characters of a request/response body in compact output. Default 4096; 0 means unlimited. */
    public static final String AI_BODY_MAX_PROPERTY = "entur.logging.ai.request-response.max-body-size";

    public static final int DEFAULT_BODY_MAX = 4096;

    /** Separates the body from the rest of a compact request-response message. */
    public static final String BODY_MARKER = " | body: ";

    public static String truncateBody(String body) {
        int max = Integer.getInteger(AI_BODY_MAX_PROPERTY, DEFAULT_BODY_MAX);
        if (max > 0 && body.length() > max) {
            return body.substring(0, max) + "...[truncated, " + body.length() + " chars]";
        }
        return body;
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
}
