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

    /** JVM system property; max number of characters of a request/response body in compact output. Unlimited if not set (or 0). */
    public static final String AI_BODY_MAX_PROPERTY = "entur.logging.ai.body.size.max";

    public static String truncateBody(String body) {
        int max = Integer.getInteger(AI_BODY_MAX_PROPERTY, 0);
        if (max > 0 && body.length() > max) {
            return body.substring(0, max) + "...[truncated, " + body.length() + " chars]";
        }
        return body;
    }
}
