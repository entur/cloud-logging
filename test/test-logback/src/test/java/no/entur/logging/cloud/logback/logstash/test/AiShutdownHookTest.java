package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

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
    public void leavesStoppingToSpringBoot() {
        LoggerContext context = new LoggerContext();
        context.start();
        context.putObject(AiShutdownHook.SPRING_BOOT_LOGGING_SYSTEM, new Object());

        hook(context).run();

        assertThat(context.isStarted()).isTrue();
        context.stop();
    }

    private static AiShutdownHook hook(LoggerContext context) {
        AiShutdownHook hook = new AiShutdownHook();
        hook.setContext(context);
        return hook;
    }
}
