package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.LoggerContext;

public class AiShutdownHookTest {

    @Test
    public void stopsLoggingWithoutSpringBoot() {
        LoggerContext context = new LoggerContext();
        context.start();

        hook(context).run();

        assertThat(context.isStarted()).isFalse();
    }

    @Test
    public void waitsForSpringBootToStopLogging() throws InterruptedException {
        LoggerContext context = springBootContext();
        // as Spring Boot's shutdown hook: closes the application contexts, then stops logging
        AtomicBoolean stoppedBySpringBoot = new AtomicBoolean();
        Thread springBoot = new Thread(() -> {
            sleep(200);
            stoppedBySpringBoot.set(context.isStarted());
            context.stop();
        }, AiShutdownHook.SPRING_BOOT_SHUTDOWN_HOOK);
        springBoot.start();

        hook(context).run();

        assertThat(stoppedBySpringBoot.get()).isTrue();
        assertThat(context.isStarted()).isFalse();
        assertThat(springBoot.isAlive()).isFalse();
    }

    @Test
    public void stopsLoggingIfSpringBootDoesNot() {
        // logging.register-shutdown-hook=false: Spring Boot's shutdown hook runs, but does not stop logging
        LoggerContext context = springBootContext();
        new Thread(() -> sleep(100), AiShutdownHook.SPRING_BOOT_SHUTDOWN_HOOK).start();

        hook(context).run();

        assertThat(context.isStarted()).isFalse();
    }

    @Test
    public void stopsLoggingWithoutSpringBootShutdownHook() {
        LoggerContext context = springBootContext();

        AiShutdownHook hook = hook(context);
        hook.hookStartMillis = 50;
        hook.run();

        assertThat(context.isStarted()).isFalse();
    }

    private static LoggerContext springBootContext() {
        LoggerContext context = new LoggerContext();
        context.start();
        context.putObject(AiShutdownHook.SPRING_BOOT_LOGGING_SYSTEM, new Object());
        return context;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static AiShutdownHook hook(LoggerContext context) {
        AiShutdownHook hook = new AiShutdownHook();
        hook.setContext(context);
        return hook;
    }
}
