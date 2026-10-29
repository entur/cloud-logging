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
}
