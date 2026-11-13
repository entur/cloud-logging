package no.entur.logging.cloud.logback.logstash.test.ai;

/**
 * AI mode: compact test logging (written to file) meant for AI tooling. The settings of the compact file are in {@code CompactSettings}.
 */
public class AiLoggingControl {

    /** JVM system property; if 'true', test logging is written as compact output meant for AI tooling. */
    public static final String AI_ENABLED_PROPERTY = "entur.logging.ai.enabled";

    /** Exactly <code>true</code>, as for the condition in <code>logback-test.xml</code> (which is case-sensitive). */
    public static boolean isAiEnabled() {
        return "true".equals(System.getProperty(AI_ENABLED_PROPERTY));
    }

    /** JVM system property; the AI files are this value followed by <code>.compact</code>, <code>.full</code> and <code>.summary</code>. */
    public static final String AI_FILE_PROPERTY = "entur.logging.ai.file";

    /** As the default in <code>logback-ai.xml</code>: <code>entur-logging-ai</code> in the temp directory. */
    public static String aiFile() {
        String file = System.getProperty(AI_FILE_PROPERTY);
        return file != null && !file.isEmpty() ? file : System.getProperty("java.io.tmpdir") + "/entur-logging-ai";
    }

    /**
     * The files of this JVM without extension, <code>&lt;file&gt;.&lt;pid&gt;</code>: every test JVM writes its own compact and full file
     * (see {@link AiPidPropertyDefiner}). The summary file, <code>&lt;file&gt;.summary</code>, is shared.
     */
    public static String aiJvmFile() {
        return aiFile() + "." + ProcessHandle.current().pid();
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

    /**
     * JVM system property; set to <code>false</code> to keep UUIDs (in MDC values and messages) and hex ids (in MDC values) instead of
     * replacing them with short aliases (u1, h1, ...). Default true.
     */
    public static final String AI_ALIAS_PROPERTY = "entur.logging.ai.alias";

    public static boolean isAliasEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(AI_ALIAS_PROPERTY));
    }
}
