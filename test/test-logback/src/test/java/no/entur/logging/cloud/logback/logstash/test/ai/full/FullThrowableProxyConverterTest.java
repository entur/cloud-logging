package no.entur.logging.cloud.logback.logstash.test.ai.full;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.LoggingEvent;

public class FullThrowableProxyConverterTest {

    private static Exception thrown(String message, Throwable cause) {
        try {
            throw new IllegalStateException(message, cause);
        } catch (IllegalStateException e) {
            return e;
        }
    }

    /** The trace of <code>%ex</code>, each line without indentation, joined with the separator. */
    private static String expected(LoggerContext context, LoggingEvent event) {
        ThrowableProxyConverter converter = new ThrowableProxyConverter();
        converter.setContext(context);
        converter.start();
        return converter.convert(event).lines().map(String::strip).filter(line -> !line.isEmpty()).map(line -> " | " + line).collect(Collectors.joining());
    }

    @Test
    public void theWholeTraceAsLogbackPrintsItButOnOneLine() {
        // causes, a suppressed exception with its own cause, and frames in common with the enclosing exception
        Exception root = thrown("root", null);
        Exception middle = thrown("middle", root);
        middle.addSuppressed(thrown("suppressed", thrown("cause of suppressed", null)));
        Exception top = thrown("top", middle);

        LoggerContext context = new LoggerContext();
        FullThrowableProxyConverter converter = new FullThrowableProxyConverter();
        converter.setContext(context);
        converter.start();
        LoggingEvent event = new LoggingEvent("f", context.getLogger("x"), Level.ERROR, "m", top, null);

        String trace = converter.convert(event);

        assertThat(trace).isEqualTo(expected(context, event));
        assertThat(trace).doesNotContain("\n");
        assertThat(trace).startsWith(" | java.lang.IllegalStateException: top | at ");
        assertThat(trace).contains(" | Caused by: java.lang.IllegalStateException: middle | at ");
        assertThat(trace).contains(" | Suppressed: java.lang.IllegalStateException: suppressed | at ");
        assertThat(trace).contains(" | Caused by: java.lang.IllegalStateException: cause of suppressed | ");
        assertThat(trace).containsMatch(" \\| \\.\\.\\. \\d+ common frames omitted");
    }

    @Test
    public void exceptionMessagesAreOnOneLine() {
        LoggerContext context = new LoggerContext();
        FullThrowableProxyConverter converter = new FullThrowableProxyConverter();
        converter.setContext(context);
        converter.start();

        String trace = converter.convert(new LoggingEvent("f", context.getLogger("x"), Level.ERROR, "m", thrown("first line\n  second line", null), null));

        assertThat(trace).startsWith(" | java.lang.IllegalStateException: first line second line | at ");
        assertThat(trace).doesNotContain("\n");
    }
}
