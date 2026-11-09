package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.core.hook.ShutdownHookBase;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Stops logging at JVM exit in AI mode, so that queued events and pending <code>repeated N more times</code> lines (see
 * {@link DeduplicatingAppender}) are written also in test JVMs without Spring Boot. If Spring Boot has configured logging in this JVM,
 * this hook first waits for Spring Boot's shutdown hook, which stops logging after closing all its application contexts (stopping
 * earlier would lose their shutdown events), and then stops logging only if Spring Boot did not (with
 * <code>logging.register-shutdown-hook=false</code>, or without Spring Boot's shutdown hook). (Spring Boot removes its marker from the
 * logger context when a context is closed, for example by <code>@DirtiesContext</code>, while other cached contexts may still be open,
 * so the marker alone is not enough.)
 */
public class AiShutdownHook extends ShutdownHookBase {

    /** Key of the object Spring Boot puts into the logger context while it manages logging. */
    static final String SPRING_BOOT_LOGGING_SYSTEM = "org.springframework.boot.logging.LoggingSystem";

    private static final String SPRING_BOOT_LOGGING_PACKAGE = "org.springframework.boot.logging.";

    /** Name of the thread of Spring Boot's shutdown hook (<code>SpringApplicationShutdownHook</code>). */
    static final String SPRING_BOOT_SHUTDOWN_HOOK = "SpringApplicationShutdownHook";

    private static volatile boolean configuredBySpringBoot;

    // shutdown hooks are started one after the other, so Spring Boot's may not have been started yet
    long hookStartMillis = 500;
    long hookMaxMillis = 60_000;

    public AiShutdownHook() {
        // created while logback-test.xml is loaded, by logback itself or by Spring Boot's logging system
        if (StackWalker.getInstance().walk(frames -> frames.anyMatch(frame -> frame.getClassName().startsWith(SPRING_BOOT_LOGGING_PACKAGE)))) {
            configuredBySpringBoot = true;
        }
    }

    @Override
    public void run() {
        if (configuredBySpringBoot || getContext().getObject(SPRING_BOOT_LOGGING_SYSTEM) != null) {
            if (isLoggingStarted()) {
                waitFor(springBootShutdownHook());
            }
            if (!isLoggingStarted()) {
                addInfo("Logging was stopped by Spring Boot");
                return;
            }
        }
        stop();
    }

    private boolean isLoggingStarted() {
        return getContext() instanceof LifeCycle lifeCycle && lifeCycle.isStarted();
    }

    private Thread springBootShutdownHook() {
        long deadline = System.nanoTime() + hookStartMillis * 1_000_000;
        while (true) {
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (SPRING_BOOT_SHUTDOWN_HOOK.equals(thread.getName()) && thread != Thread.currentThread()) {
                    return thread;
                }
            }
            if (System.nanoTime() > deadline || !isLoggingStarted()) {
                return null;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    private void waitFor(Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(hookMaxMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static boolean isConfiguredBySpringBoot() {
        return configuredBySpringBoot;
    }
}
