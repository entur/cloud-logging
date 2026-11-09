package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.core.hook.ShutdownHookBase;

/**
 * Stops logging at JVM exit in AI mode, so that queued events and pending <code>repeated N more times</code> lines (see
 * {@link DeduplicatingAppender}) are written also in test JVMs without Spring Boot. If Spring Boot manages logging, it is left to
 * Spring Boot, which stops logging after closing its application contexts; stopping earlier would lose their shutdown events.
 */
public class AiShutdownHook extends ShutdownHookBase {

    /** Key of the object Spring Boot puts into the logger context while it manages logging. */
    static final String SPRING_BOOT_LOGGING_SYSTEM = "org.springframework.boot.logging.LoggingSystem";

    @Override
    public void run() {
        if (getContext().getObject(SPRING_BOOT_LOGGING_SYSTEM) != null) {
            addInfo("Logging is stopped by Spring Boot");
            return;
        }
        stop();
    }
}
