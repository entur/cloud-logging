package no.entur.logging.cloud.logback.logstash.test.ai;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class AiTestRunTest {

    private static final Instant T0 = Instant.parse("2026-11-11T10:00:00Z");

    private static Instant at(int millis) {
        return T0.plusMillis(millis);
    }

    private final AiTestRun run = new AiTestRun();

    private void classStarted(String id, String name, String thread, int at) {
        run.testStarted(id, null, name, false, true, thread, at(at));
    }

    private void test(String id, String classId, String name, String thread, int start, int end, String failure) {
        run.testStarted(id, classId, name, true, false, thread, at(start));
        run.testFinished(id, at(end), failure != null, failure);
    }

    private void event(int at, String thread, String logger, String message) {
        run.event(at(at), thread, logger, "WARN", "WARN " + logger + " #" + message.hashCode() + " : " + message);
    }

    @Test
    public void failedTestGetsTheEventsLoggedWhileItRan() {
        classStarted("[c]", "MyTest", "main", 0);
        event(5, "main", "Setup", "in @BeforeAll");
        test("[c]/[ok]", "[c]", "MyTest > passes()", "main", 10, 20, null);
        test("[c]/[m]", "[c]", "MyTest > fails()", "main", 30, 40, "expected: <1> but was: <0>");
        event(32, "pool-1-thread-1", "Parser", "on another thread");
        event(35, "main", "Service", "on the test thread");
        event(50, "main", "Later", "after the test");

        String report = run.report();

        assertThat(report).startsWith("1 test failed. WARN/ERROR events logged while they ran");
        assertThat(report).contains("MyTest > fails(): expected: <1> but was: <0>\n");
        assertThat(report).doesNotContain("passes()");
        assertThat(report).doesNotContain("after the test");
        // own thread first, then the others (from the start of the class, so that its set-up counts)
        assertThat(report).containsMatch("on the test thread\n.*in @BeforeAll\n.*on another thread\n");
        assertThat(report).doesNotContain("other tests ran at the same time");
    }

    @Test
    public void eventsOfEarlierTestsOfTheClassAreNotIncluded() {
        classStarted("[c]", "MyTest", "main", 0);
        test("[c]/[a]", "[c]", "MyTest > a()", "main", 10, 20, null);
        event(15, "pool-1-thread-1", "Parser", "logged while a ran");
        event(25, "main", "Setup", "between the tests");
        test("[c]/[b]", "[c]", "MyTest > b()", "main", 30, 40, "boom");

        String report = run.report();

        assertThat(report).doesNotContain("logged while a ran");
        assertThat(report).contains("between the tests");
    }

    @Test
    public void eventsOfOtherThreadsAreMarkedWhenOtherTestsRanAtTheSameTime() {
        classStarted("[c]", "MyTest", "main", 0);
        test("[c]/[a]", "[c]", "MyTest > a()", "worker-1", 10, 40, "boom");
        test("[c]/[b]", "[c]", "MyTest > b()", "worker-2", 15, 30, null);
        event(20, "worker-1", "A", "from a");
        event(25, "worker-2", "B", "from b");

        String report = run.report();

        // b's event was logged on b's thread while b ran, so it is b's
        assertThat(report).contains("from a\n");
        assertThat(report).doesNotContain("from b");
        assertThat(report).doesNotContain("threads of no test");
    }

    @Test
    public void eventsOfThreadsOfNoTestAreMarkedWhenOtherTestsRanAtTheSameTime() {
        classStarted("[c]", "MyTest", "main", 0);
        test("[c]/[a]", "[c]", "MyTest > a()", "worker-1", 10, 40, "boom");
        test("[c]/[b]", "[c]", "MyTest > b()", "worker-2", 15, 30, null);
        event(20, "worker-1", "A", "from a");
        event(25, "pool-1-thread-1", "Parser", "from a pool");

        String report = run.report();

        assertThat(report).containsMatch("from a\n  logged on threads of no test while 1 other test ran at the same time, so they may belong to those:\n.*from a pool\n");
    }

    @Test
    public void testsWithTheSameEventsAreNotRepeated() {
        classStarted("[c]", "MyTest", "main", 0);
        event(5, "main", "Context", "context failed to start");
        test("[c]/[a]", "[c]", "MyTest > a()", "main", 10, 20, "IllegalStateException: Failed to load ApplicationContext");
        test("[c]/[b]", "[c]", "MyTest > b()", "main", 30, 40, "IllegalStateException: Failed to load ApplicationContext");

        String report = run.report();

        assertThat(report).startsWith("2 tests failed.");
        assertThat(report).contains("MyTest > b(): IllegalStateException: Failed to load ApplicationContext\n  (the same events as the test above)\n");
    }

    @Test
    public void identicalEventsAreShownOnceWithTheirCount() {
        test("[m]", null, "MyTest > fails()", "main", 0, 100, "boom");
        run.event(at(10), "main", "Parser", "WARN", "WARN Parser #000000000001 : could not parse file a");
        run.event(at(20), "main", "Parser", "WARN", "WARN Parser #000000000002 : could not parse file a");
        run.event(at(30), "main", "Parser", "WARN", "WARN Parser #000000000003 : could not parse file b");

        String report = run.report();

        assertThat(report).contains("  WARN Parser #000000000001 : could not parse file a (2 times)\n  WARN Parser #000000000003 : could not parse file b\n");
        assertThat(report).doesNotContain("#000000000002");
    }

    @Test
    public void testWithoutEventsPointsToTheCompactFile() {
        test("[m]", null, "MyTest > fails()", "main", 10, 20, "expected: <true> but was: <false>");

        assertThat(run.report()).contains("  (no WARN/ERROR events while this test ran; look in the compact file)\n");
    }

    @Test
    public void eventsPerTestAreLimited() {
        test("[m]", null, "MyTest > fails()", "main", 0, 100, "boom");
        for (int i = 0; i < AiTestRun.MAX_EVENTS_PER_TEST + 2; i++) {
            event(10 + i, "main", "Logger", "event " + i);
        }

        String report = run.report();

        assertThat(report).contains("event " + (AiTestRun.MAX_EVENTS_PER_TEST - 1) + "\n");
        assertThat(report).doesNotContain("event " + AiTestRun.MAX_EVENTS_PER_TEST + "\n");
        assertThat(report).contains("  ... and 2 more WARN/ERROR events while this test ran\n");
    }

    @Test
    public void longLinesAreCut() {
        test("[m]", null, "MyTest > fails()", "main", 0, 100, "boom");
        event(10, "main", "Logger", "x".repeat(AiTestRun.MAX_LINE_LENGTH));

        assertThat(run.report()).containsMatch("x{10}\\.\\.\\.\n");
    }

    @Test
    public void withoutFailuresTheEventsAreCountedPerLogger() {
        test("[m]", null, "MyTest > passes()", "main", 0, 100, null);
        event(10, "main", "Parser", "one");
        event(20, "main", "Parser", "two");
        event(30, "main", "Service", "three");

        assertThat(run.report()).isEqualTo("No tests failed. WARN/ERROR events per logger:\n      2 WARN Parser\n      1 WARN Service\n");

        run.clear();
        assertThat(run.report()).isEqualTo("No tests failed. No WARN/ERROR events.\n");
    }

    @Test
    public void onlyTheLastEventsAreKept() {
        test("[m]", null, "MyTest > fails()", "main", 0, 100, "boom");
        for (int i = 0; i <= AiTestRun.MAX_EVENTS; i++) {
            event(50, "main", "Logger", "event");
        }

        assertThat(run.report()).contains("(the first 1 events of the run are not kept, only the last " + AiTestRun.MAX_EVENTS + ")\n");
    }

    @Test
    public void failureIsTheFirstLineOfTheAssertionOrTheExceptionWithItsName() {
        assertThat(AiTestExecutionListener.failure(new AssertionError("expected: <1> but was: <0>\nmore"))).isEqualTo("expected: <1> but was: <0>");
        assertThat(AiTestExecutionListener.failure(new IllegalStateException("boom"))).isEqualTo("IllegalStateException: boom");
        assertThat(AiTestExecutionListener.failure(new NullPointerException())).isEqualTo("NullPointerException");
    }

    @Test
    public void loggerIsShortenedToTheSimpleClassName() {
        assertThat(FailuresFileAppender.simpleName("no.entur.abt.TimetableBatchParser")).isEqualTo("TimetableBatchParser");
        assertThat(FailuresFileAppender.simpleName("org.springframework.web")).isEqualTo("org.springframework.web");
    }
}
