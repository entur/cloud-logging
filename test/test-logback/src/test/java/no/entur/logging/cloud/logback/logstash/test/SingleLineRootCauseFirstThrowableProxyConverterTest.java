package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class SingleLineRootCauseFirstThrowableProxyConverterTest {

    @AfterEach
    public void cleanUp() {
        System.clearProperty(CompactSettings.AI_MESSAGE_MAX_PROPERTY);
        System.clearProperty(CompactSettings.AI_STACKTRACE_MAX_LINES_PROPERTY);
        System.clearProperty(CompactSettings.AI_STACKTRACE_MAX_CAUSES_PROPERTY);
    }

    private static Exception exception(String message, Exception cause, String... classes) {
        Exception e = cause == null ? new IllegalStateException(message) : new IllegalStateException(message, cause);
        StackTraceElement[] trace = new StackTraceElement[classes.length];
        for (int i = 0; i < classes.length; i++) {
            trace[i] = new StackTraceElement(classes[i], "m", classes[i].substring(classes[i].lastIndexOf('.') + 1) + ".java", i + 1);
        }
        e.setStackTrace(trace);
        return e;
    }

    private static String convert(Throwable throwable, String... options) {
        LoggerContext context = new LoggerContext();
        SingleLineRootCauseFirstThrowableProxyConverter converter = new SingleLineRootCauseFirstThrowableProxyConverter();
        converter.setContext(context);
        converter.setOptionList(List.of(options));
        converter.start();
        return converter.convert(new LoggingEvent("f", context.getLogger("x"), Level.ERROR, "m", throwable, null));
    }

    @Test
    public void framesOfNamedClassLoadersAndVersionedModulesAreFrames() {
        Exception e = new IllegalStateException("boom");
        e.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("loader", "com.example.app", "1.2.3+4", "com.example.App", "run", "App.java", 42),
                new StackTraceElement(null, "com.example.app", "1.2.3", "com.example.App", "main", "App.java", 7),
                new StackTraceElement("a.App2", "m", "App2.java", 3)});

        // 2 frames per exception by default: the third is left out, so the first two count as frames
        assertThat(convert(e, "full")).isEqualTo(" | java.lang.IllegalStateException: boom"
                + " | at loader/com.example.app@1.2.3+4/com.example.App.run(App.java:42)"
                + " | at com.example.app@1.2.3/com.example.App.main(App.java:7) | [... 1 frames omitted]");
    }

    @Test
    public void controlCharactersAreEscapedInBothFiles() {
        Exception e = exception("bad\u0000value\u001b[31m", null, "a.App");

        assertThat(convert(e, "full")).isEqualTo(" | java.lang.IllegalStateException: bad\\u0000value\\u001b[31m | at a.App.m(App.java:1)");

        LoggerContext context = new LoggerContext();
        FullThrowableProxyConverter full = new FullThrowableProxyConverter();
        full.setContext(context);
        full.start();
        String trace = full.convert(new LoggingEvent("f", context.getLogger("x"), Level.ERROR, "m", e, null));
        assertThat(trace).startsWith("java.lang.IllegalStateException: bad\\u0000value\\u001b[31m" + System.lineSeparator() + "\tat a.App.m(App.java:1)");
        assertThat(full.convert(new LoggingEvent("f", context.getLogger("x"), Level.ERROR, "m", null, null))).isEmpty();
    }

    @Test
    public void suppressedExceptionsAndTheirCausesDoNotCountAsCauses() {
        System.setProperty(CompactSettings.AI_STACKTRACE_MAX_LINES_PROPERTY, "1");
        System.setProperty(CompactSettings.AI_STACKTRACE_MAX_CAUSES_PROPERTY, "3");
        Exception root = exception("R", null, "a.R");
        Exception middle = exception("T", root, "a.T");
        middle.addSuppressed(exception("S", exception("S0", null, "a.S0"), "a.S"));
        Exception top = exception("U", middle, "a.U");

        // logback: R, Wrapped by T, (indented) Suppressed S0, Wrapped by S, then Wrapped by U
        assertThat(convert(top, "full")).endsWith(" | Wrapped by: java.lang.IllegalStateException: U | at a.U.m(U.java:1)");
    }

    @Test
    public void longExceptionMessagesAreCut() {
        System.setProperty(CompactSettings.AI_MESSAGE_MAX_PROPERTY, "60");
        // the first line has 54 characters, so 5 of the second line fit
        Exception e = exception("first line " + "x".repeat(10) + "\nsecond line " + "y".repeat(30), null, "a.App");

        assertThat(convert(e, "full")).isEqualTo(" | java.lang.IllegalStateException: first line xxxxxxxxxx"
                + " secon...[truncated] | at a.App.m(App.java:1)");
        assertThat(convert(exception("z".repeat(50), null, "a.App"), "full"))
                .isEqualTo(" | java.lang.IllegalStateException: " + "z".repeat(27) + "...[truncated, 83 chars] | at a.App.m(App.java:1)");
    }

    @Test
    public void framesAreLimitedAndLogbackMarkersDropped() {
        Exception e = exception("boom", null, "a.App", "org.fw.X", "org.fw.Y", "a.App2", "a.App3", "a.App4");

        assertThat(convert(e, "full", "org.fw"))
                .isEqualTo(" | java.lang.IllegalStateException: boom | at a.App.m(App.java:1) | at a.App2.m(App2.java:4) | [... 2 frames omitted]");
    }

    @Test
    public void rootCauseFirstWithMaxCauses() {
        System.setProperty(CompactSettings.AI_STACKTRACE_MAX_LINES_PROPERTY, "1");
        System.setProperty(CompactSettings.AI_STACKTRACE_MAX_CAUSES_PROPERTY, "2");
        Exception root = exception("root\nsecond line", null, "a.Root", "a.Root2");
        Exception middle = exception("middle", root, "a.Middle");
        Exception top = exception("top", middle, "a.Top");

        assertThat(convert(top, "full"))
                .isEqualTo(" | java.lang.IllegalStateException: root second line | at a.Root.m(Root.java:1) | [... 1 frames omitted]"
                        + " | Wrapped by: java.lang.IllegalStateException: middle | at a.Middle.m(Middle.java:1) | [... more causes omitted]");
    }

    @Test
    public void messageLinesStartingWithAtAreNotFrames() {
        System.setProperty(CompactSettings.AI_STACKTRACE_MAX_LINES_PROPERTY, "1");
        Exception e = exception("Unexpected character\n at [Source: (String)\"{x\"; line: 1, column: 2]", null, "a.App", "a.App2");

        assertThat(convert(e, "full"))
                .isEqualTo(" | java.lang.IllegalStateException: Unexpected character at [Source: (String)\"{x\"; line: 1, column: 2]"
                        + " | at a.App.m(App.java:1) | [... 1 frames omitted]");
    }

    @Test
    public void suppressedExceptionsDoNotCountAsCauses() {
        System.setProperty(CompactSettings.AI_STACKTRACE_MAX_LINES_PROPERTY, "1");
        System.setProperty(CompactSettings.AI_STACKTRACE_MAX_CAUSES_PROPERTY, "2");
        Exception root = exception("root", null, "a.Root");
        root.addSuppressed(exception("close 1", null, "a.Close"));
        root.addSuppressed(exception("close 2", null, "a.Close"));
        Exception top = exception("top", root, "a.Top");

        String converted = convert(top, "full");
        assertThat(converted).contains("Suppressed: java.lang.IllegalStateException: close 2");
        assertThat(converted).contains("Wrapped by: java.lang.IllegalStateException: top | at a.Top.m(Top.java:1)");
        assertThat(converted).doesNotContain("more causes omitted");
    }

    @Test
    public void noExceptionGivesEmptyString() {
        assertThat(convert(null, "full")).isEmpty();
    }
}
