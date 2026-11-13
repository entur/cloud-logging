package no.entur.logging.cloud.logback.logstash.test.ai.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

/**
 * Test classes which {@link AiTestExecutionListenerTest} runs with its own launcher. They fail on purpose, so they are disabled in the
 * test run of the build.
 */
public class Fixtures {

    /** Configuration parameter set by the launcher of {@link AiTestExecutionListenerTest}. */
    static final String ENABLED = "entur.logging.ai.test.fixtures";

    /** Logs to the files of the test, not to the logging of the build. */
    static volatile Logger logger;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @ExtendWith(FixtureCondition.class)
    @interface Fixture {
    }

    static class FixtureCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return context.getConfigurationParameter(ENABLED).isPresent() ? ConditionEvaluationResult.enabled("fixture")
                    : ConditionEvaluationResult.disabled("only run by AiTestExecutionListenerTest");
        }
    }

    @Fixture
    @TestMethodOrder(MethodOrderer.MethodName.class)
    public static class MixedFixture {

        @Test
        public void a_passesWithoutLogging() {
        }

        @Test
        public void b_logsAndFails() {
            logger.info("loading document");
            logger.warn("document service is slow");
            assertEquals(404, 500);
        }

        @Test
        public void c_logsAndPasses() {
            logger.warn("document service is slow");
            logger.warn("document service is slow");
        }

        @Test
        public void d_failsWithoutLogging() {
            throw new IllegalStateException("outer\nsecond line", new IllegalArgumentException("the root"));
        }

        @Test
        @Disabled
        public void e_disabled() {
        }

        @Test
        public void f_aborted() {
            assumeTrue(false);
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2 })
        public void g_parameterized(int value) {
            assertEquals(1, value);
        }

        @Test
        @DisplayName("returns not found")
        public void h_displayName() {
            helper();
        }

        @Test
        public void i_sameFailure() {
            helper();
        }

        private static void helper() {
            throw new UnsupportedOperationException("not yet");
        }
    }

    @Fixture
    public static class BrokenSetUpFixture {

        @BeforeAll
        public static void setUp() {
            logger.error("unable to start");
            throw new IllegalStateException("no database");
        }

        @Test
        public void first() {
        }

        @Test
        public void second() {
        }
    }

    /** Two tests which run at the same time: each waits until the other has started, logs, and waits until the other has logged. */
    @Fixture
    @Execution(ExecutionMode.CONCURRENT)
    public static class ParallelFixture {

        private static final CyclicBarrier STARTED = new CyclicBarrier(2);
        private static final CyclicBarrier LOGGED = new CyclicBarrier(2);

        @Test
        public void fails() throws Exception {
            STARTED.await(30, TimeUnit.SECONDS);
            logger.warn("the failing test");
            // an executor of the code under test, for example
            Thread thread = new Thread(() -> logger.warn("from another thread"));
            thread.start();
            thread.join();
            LOGGED.await(30, TimeUnit.SECONDS);
            throw new IllegalStateException("failed");
        }

        @Test
        public void passes() throws Exception {
            STARTED.await(30, TimeUnit.SECONDS);
            logger.warn("the other test");
            LOGGED.await(30, TimeUnit.SECONDS);
        }
    }

    /** Seven tests which fail for the same reason, which is only in the log, and two which pass; all log the same unrelated warning. */
    @Fixture
    public static class SharedCauseFixture {

        private static void store(int document) {
            try {
                throw new IllegalStateException("no connection");
            } catch (IllegalStateException e) {
                logger.warn("Unable to store document " + document, e);
            }
        }

        private static void lookup(int document, boolean stored) {
            logger.warn("Slow response");
            if (stored) {
                return;
            }
            store(document);
            assertEquals("document " + document, null);
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7 })
        public void missing(int document) {
            lookup(document, false);
        }

        @ParameterizedTest
        @ValueSource(ints = { 8, 9 })
        public void present(int document) {
            lookup(document, true);
        }
    }

    @Fixture
    public static class PassingFixture {

        @Test
        public void passes() {
        }
    }
}
