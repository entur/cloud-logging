package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.core.hook.ShutdownHookBase;

/**
 * Stops logging at JVM exit in AI mode, so that queued events and pending <code>repeated N more times</code> lines (see
 * {@link DeduplicatingAppender}) are written also in test JVMs without Spring Boot. If Spring Boot has configured logging in this JVM,
 * stopping is left to Spring Boot, which stops logging after closing all its application contexts; stopping earlier would lose
 * their shutdown events. (Spring Boot removes its marker from the logger context when a context is closed, for example by
 * <code>@DirtiesContext</code>, while other cached contexts may still be open, so the marker alone is not enough.)
 */
public class AiShutdownHook extends ShutdownHookBase {

    /** Key of the object Spring Boot puts into the logger context while it manages logging. */
    static final String SPRING_BOOT_LOGGING_SYSTEM = "org.springframework.boot.logging.LoggingSystem";

    private static final String SPRING_BOOT_LOGGING_PACKAGE = "org.springframework.boot.logging.";

    private static volatile boolean configuredBySpringBoot;

    public AiShutdownHook() {
        // created while logback-test.xml is loaded, by logback itself or by Spring Boot's logging system
        if (StackWalker.getInstance().walk(frames -> frames.anyMatch(frame -> frame.getClassName().startsWith(SPRING_BOOT_LOGGING_PACKAGE)))) {
            configuredBySpringBoot = true;
        }
    }

    @Override
    public void run() {
        if (configuredBySpringBoot || getContext().getObject(SPRING_BOOT_LOGGING_SYSTEM) != null) {
            addInfo("Logging is stopped by Spring Boot");
            return;
        }
        stop();
    }

    static boolean isConfiguredBySpringBoot() {
        return configuredBySpringBoot;
    }
}
