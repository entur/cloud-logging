package no.entur.logging.cloud.logback.logstash.test.ai.full;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.CoreConstants;
import no.entur.logging.cloud.logback.logstash.test.ai.AiFileAppender;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiLevelConverter;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiTestConverter;

/** The full file as logback-ai.xml writes it: nothing is cut, however long a message or a stack trace is. */
public class FullFileTest {

    /** The pattern of the full file in logback-ai.xml. */
    private static final String PATTERN = "%d{HH:mm:ss.SSS} %-5aiLevel [%thread] %logger%aiTest%fMdc%fEventId : %fMsg%fFields%fEx%n";

    private static final int DEPTH = 300;

    @Test
    public void longMessagesAndDeepStackTracesAreNotCut(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("test.full");
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, new HashMap<>(Map.of("aiTest", AiTestConverter.class.getName(),
                "aiLevel", AiLevelConverter.class.getName(), "fMdc", FullMdcConverter.class.getName(), "fEventId",
                FullEventIdConverter.class.getName(), "fMsg", FullMessageConverter.class.getName(), "fFields",
                FullFieldsConverter.class.getName(), "fEx", FullThrowableProxyConverter.class.getName())));
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(PATTERN);
        encoder.start();
        AiFileAppender appender = new AiFileAppender();
        appender.setContext(context);
        appender.setFile(file.toString());
        appender.setEncoder(encoder);
        appender.start();
        Logger logger = context.getLogger("app");
        logger.addAppender(appender);

        String message = "first line\n" + "x".repeat(20_000) + " end of message";
        String rootMessage = "root cause " + "y".repeat(10_000) + " end of root cause";
        RuntimeException exception;
        try {
            deep(DEPTH, rootMessage);
            throw new AssertionError("not thrown");
        } catch (IllegalStateException e) {
            exception = new RuntimeException("wrapper", e);
        }
        logger.error(message, exception);
        context.stop();

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(line -> line.contains("end of message")).toList();
        // one line, with the whole message, the whole root cause message and every frame
        assertThat(lines).hasSize(1);
        String line = lines.get(0);
        assertThat(line).contains("first line");
        assertThat(line).contains("x".repeat(20_000) + " end of message");
        assertThat(line).contains("Caused by: java.lang.IllegalStateException: " + rootMessage);
        int frames = line.split("FullFileTest\\.deep\\(", -1).length - 1;
        assertThat(frames).isAtLeast(DEPTH);
        assertThat(line.length()).isGreaterThan(30_000 + DEPTH * 50);
    }

    private static void deep(int depth, String message) {
        if (depth == 0) {
            throw new IllegalStateException(message);
        }
        deep(depth - 1, message);
    }
}
