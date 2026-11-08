package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;

public class CompactFileTest {

    @AfterEach
    public void cleanUp() {
        System.clearProperty(AiLoggingControl.AI_QUIET_LOGGERS_PROPERTY);
    }

    @Test
    public void loggerIsShortenedToSimpleClassName() {
        assertThat(CompactLoggerConverter.shorten("org.springframework.web.servlet.DispatcherServlet")).isEqualTo("DispatcherServlet");
        assertThat(CompactLoggerConverter.shorten("a.b.Outer$Inner")).isEqualTo("Outer$Inner");
        assertThat(CompactLoggerConverter.shorten("no.entur.logging.cloud")).isEqualTo("no.entur.logging.cloud");
        assertThat(CompactLoggerConverter.shorten("ROOT")).isEqualTo("ROOT");
    }

    @Test
    public void quietLoggersAreFilteredBelowWarn() {
        LoggerContext context = new LoggerContext();
        QuietLoggersFilter filter = new QuietLoggersFilter();
        filter.start();

        assertThat(filter.decide(event(context, "org.apache.catalina.core.StandardService", Level.INFO))).isEqualTo(FilterReply.DENY);
        assertThat(filter.decide(event(context, "org.apache.catalina.core.StandardService", Level.WARN))).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(event(context, "org.apache.catalinax.Other", Level.INFO))).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(event(context,
                "org.springframework.security.config.annotation.authentication.configuration.InitializeUserDetailsBeanManagerConfigurer$Inner",
                Level.INFO))).isEqualTo(FilterReply.DENY);
        assertThat(filter.decide(event(context, "com.example.App", Level.INFO))).isEqualTo(FilterReply.NEUTRAL);

        System.setProperty(AiLoggingControl.AI_QUIET_LOGGERS_PROPERTY, "");
        QuietLoggersFilter none = new QuietLoggersFilter();
        none.start();
        assertThat(none.decide(event(context, "org.apache.catalina.core.StandardService", Level.INFO))).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    public void jvmStartLineIsWrittenOncePerFile(@TempDir Path directory) throws Exception {
        File file = directory.resolve("ai.log").toFile();
        LoggerContext context = new LoggerContext();
        for (int i = 0; i < 2; i++) {
            AiFileAppender appender = new AiFileAppender();
            appender.setContext(context);
            appender.setFile(file.getAbsolutePath());
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern("%level %msg%n");
            encoder.start();
            appender.setEncoder(encoder);
            appender.start();
            appender.doAppend(event(context, "x", Level.INFO));
            appender.stop();
        }

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).matches("---- JVM pid \\d+ started .* in .* \\(aliases u1, h1, \\.\\.\\. restart\\) ----");
        assertThat(lines.subList(1, 3)).containsExactly("INFO message", "INFO message");
    }

    private static LoggingEvent event(LoggerContext context, String logger, Level level) {
        LoggingEvent event = new LoggingEvent("f", context.getLogger(logger), level, "message", null, null);
        event.setMDCPropertyMap(Map.of());
        return event;
    }
}
