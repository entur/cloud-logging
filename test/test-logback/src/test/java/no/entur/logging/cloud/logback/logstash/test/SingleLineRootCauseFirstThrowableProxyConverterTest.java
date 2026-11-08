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
        System.clearProperty(AiLoggingControl.AI_STACKTRACE_MAX_LINES_PROPERTY);
        System.clearProperty(AiLoggingControl.AI_STACKTRACE_MAX_CAUSES_PROPERTY);
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
    public void framesAreLimitedAndLogbackMarkersDropped() {
        Exception e = exception("boom", null, "a.App", "org.fw.X", "org.fw.Y", "a.App2", "a.App3", "a.App4");

        assertThat(convert(e, "full", "org.fw"))
                .isEqualTo(" | java.lang.IllegalStateException: boom | at a.App.m(App.java:1) | at a.App2.m(App2.java:4) | [... 2 frames omitted]");
    }

    @Test
    public void rootCauseFirstWithMaxCauses() {
        System.setProperty(AiLoggingControl.AI_STACKTRACE_MAX_LINES_PROPERTY, "1");
        System.setProperty(AiLoggingControl.AI_STACKTRACE_MAX_CAUSES_PROPERTY, "2");
        Exception root = exception("root\nsecond line", null, "a.Root", "a.Root2");
        Exception middle = exception("middle", root, "a.Middle");
        Exception top = exception("top", middle, "a.Top");

        assertThat(convert(top, "full"))
                .isEqualTo(" | java.lang.IllegalStateException: root second line | at a.Root.m(Root.java:1) | [... 1 frames omitted]"
                        + " | Wrapped by: java.lang.IllegalStateException: middle | at a.Middle.m(Middle.java:1) | [... more causes omitted]");
    }

    @Test
    public void noExceptionGivesEmptyString() {
        assertThat(convert(null, "full")).isEmpty();
    }
}
