package no.entur.logging.cloud.logback.logstash.test.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

public class AiTestEventsTest {

    private static StackTraceElement frame(Class<?> type) {
        return new StackTraceElement(type.getName(), "m", type.getSimpleName() + ".java", 1);
    }

    @Test
    public void theFrameInTheProjectsOwnCodeIsPreferred() {
        // a library (a jar), this project (a directory of class files), a test framework
        StackTraceElement library = frame(ch.qos.logback.classic.Logger.class);
        StackTraceElement project = frame(AiTestEventsTest.class);
        StackTraceElement framework = frame(Test.class);

        assertThat(AiTestEvents.isProjectCode(AiTestEventsTest.class.getName())).isTrue();
        assertThat(AiTestEvents.isProjectCode(ch.qos.logback.classic.Logger.class.getName())).isFalse();
        assertThat(AiTestEvents.isProjectCode("no.such.Class")).isFalse();

        // an exception thrown within a library which the project called
        assertThat(AiTestEvents.frameOf(new StackTraceElement[] { framework, library, project, framework })).isEqualTo(project);
        // no project code: the library, not the framework
        assertThat(AiTestEvents.frameOf(new StackTraceElement[] { framework, library, framework })).isEqualTo(library);
        assertThat(AiTestEvents.frameOf(new StackTraceElement[] { framework })).isEqualTo(framework);
        assertThat(AiTestEvents.frameOf(new StackTraceElement[0])).isNull();
    }

    @Test
    public void theFrameOfTheClassWhichLoggedTheEvent() {
        StackTraceElement thrower = new StackTraceElement("com.example.Parser", "parse", "Parser.java", 10);
        StackTraceElement caller = new StackTraceElement("com.example.Cache", "store", "Cache.java", 20);
        StackTraceElement nested = new StackTraceElement("com.example.Cache$Loader", "load", "Cache.java", 30);
        StackTraceElement other = new StackTraceElement("com.example.CacheConfiguration", "cache", "CacheConfiguration.java", 40);
        StackTraceElement[] frames = { thrower, other, nested, caller };

        // the class itself or a class nested in it, not a class whose name only starts the same way
        assertThat(AiTestEvents.callerFrameOf(frames, "com.example.Cache")).isEqualTo(nested);
        assertThat(AiTestEvents.callerFrameOf(new StackTraceElement[] { thrower, other, caller }, "com.example.Cache")).isEqualTo(caller);
        assertThat(AiTestEvents.callerFrameOf(new StackTraceElement[] { thrower, other }, "com.example.Cache")).isNull();
        assertThat(AiTestEvents.callerFrameOf(frames, null)).isNull();
    }
}
