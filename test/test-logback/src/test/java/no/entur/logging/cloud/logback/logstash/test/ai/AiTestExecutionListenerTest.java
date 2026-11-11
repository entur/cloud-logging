package no.entur.logging.cloud.logback.logstash.test.ai;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/** Runs a sample test class through the JUnit Platform with the listener, as Gradle and Maven do. */
public class AiTestExecutionListenerTest {

    private static final String SAMPLE_PROPERTY = "entur.logging.ai.listener-sample";

    private static volatile AiTestRun sampleRun;

    @Test
    public void failedTestGetsTheEventsOfItsThreadAndTheListenerRunsOnTheTestThread() {
        AiTestRun run = new AiTestRun();
        sampleRun = run;
        System.setProperty(SAMPLE_PROPERTY, "true");
        try {
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(Sample.class)).build();
            LauncherFactory.create().execute(request, new AiTestExecutionListener(run, true));
        } finally {
            System.clearProperty(SAMPLE_PROPERTY);
            sampleRun = null;
        }

        String report = run.report();

        assertThat(report).startsWith("1 test failed.");
        assertThat(report).contains("Sample > fails(): expected the failure\n");
        // logged on the test's thread, which the listener recorded when the test started
        assertThat(report).contains("  WARN Sample : logged by the failing test on its thread\n");
        assertThat(report).doesNotContain("other threads");
        assertThat(report).doesNotContain("passes()");
        assertThat(report).doesNotContain("logged by the passing test");
    }

    @EnabledIfSystemProperty(named = SAMPLE_PROPERTY, matches = "true")
    static class Sample {

        @Test
        void passes() {
            sampleRun.event(Instant.now(), Thread.currentThread().getName(), "Sample", "WARN", "WARN Sample : logged by the passing test");
        }

        @Test
        void fails() throws InterruptedException {
            // after the listener's start time (milliseconds)
            Thread.sleep(2);
            sampleRun.event(Instant.now(), Thread.currentThread().getName(), "Sample", "WARN", "WARN Sample : logged by the failing test on its thread");
            fail("expected the failure");
        }
    }
}
