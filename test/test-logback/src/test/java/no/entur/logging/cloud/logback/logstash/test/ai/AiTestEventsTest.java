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
}
