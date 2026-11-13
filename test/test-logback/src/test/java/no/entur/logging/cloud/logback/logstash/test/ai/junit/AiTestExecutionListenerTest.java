package no.entur.logging.cloud.logback.logstash.test.ai.junit;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.CoreConstants;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAsyncAppenderLogging;
import no.entur.logging.cloud.logback.logstash.test.ai.AiFileAppender;
import no.entur.logging.cloud.logback.logstash.test.ai.FanOutAppender;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiTestConverter;
import no.entur.logging.cloud.logback.logstash.test.ai.compact.DeduplicatingAppender;

public class AiTestExecutionListenerTest {

    private static AiFileAppender fileAppender(LoggerContext context, Path file, boolean skipEmptyTests) {
        AiFileAppender appender = new AiFileAppender();
        appender.setContext(context);
        appender.setFile(file.toString());
        appender.setSkipEmptyTests(skipEmptyTests);
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%level%aiTest %msg%n");
        encoder.start();
        appender.setEncoder(encoder);
        appender.start();
        return appender;
    }

    /** The appenders of logback-ai.xml: async, then the deduplicated compact file and the full file. */
    private static LoggerContext logging(Path file, List<ILoggingEvent> observed) {
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, new HashMap<>(Map.of("aiTest", AiTestConverter.class.getName())));

        DeduplicatingAppender dedup = new DeduplicatingAppender();
        dedup.setContext(context);
        dedup.addAppender(fileAppender(context, Path.of(file + ".compact"), true));
        dedup.start();

        FanOutAppender fanOut = new FanOutAppender();
        fanOut.setContext(context);
        fanOut.addAppender(dedup);
        fanOut.addAppender(fileAppender(context, Path.of(file + ".full"), false));
        fanOut.start();

        CompositeConsoleAsyncAppenderLogging async = new CompositeConsoleAsyncAppenderLogging();
        async.setContext(context);
        async.setDiscardingThreshold(0);
        async.setMaxFlushTime(0);
        async.addAppender(fanOut);
        async.setListener(observed::add);
        async.start();

        context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(async);
        return context;
    }

    private static void run(AiTestExecutionListener listener, Class<?>... classes) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request().configurationParameter(Fixtures.ENABLED, "true")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true");
        for (Class<?> c : classes) {
            builder.selectors(selectClass(c));
        }
        LauncherDiscoveryRequest request = builder.build();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(request, listener);
    }

    /** As at JVM exit. */
    private static void writeTotals(Path file) {
        AiTestExecutionListener.writeTotals(Path.of(file + ".summary"));
    }

    private static List<String> lines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    /** The lines from the start to the end marker of the token, as the sed command of the summary prints them. */
    private static List<String> range(List<String> lines, String token) {
        List<String> range = new ArrayList<>();
        boolean in = false;
        for (String line : lines) {
            if (line.contains("#" + token + " start")) {
                in = true;
            }
            if (in) {
                range.add(line);
            }
            if (in && line.contains("#" + token + " end")) {
                break;
            }
        }
        return range;
    }

    private static String token(List<String> summary, String entry) {
        for (String line : summary) {
            Matcher matcher = Pattern.compile(Pattern.quote(entry) + " #(t\\d+)").matcher(line);
            if (line.startsWith("FAILED ") && matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new AssertionError("No entry " + entry + " in " + summary);
    }

    @Test
    public void summaryListsFailedTestsAndPointsToTheirLogLines(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("test");
        List<ILoggingEvent> observed = new ArrayList<>();
        LoggerContext context = logging(file, observed);
        Fixtures.logger = context.getLogger("fixture");
        try {
            run(new AiTestExecutionListener(true, file.toString(), file.toString(), () -> true, () -> context), Fixtures.MixedFixture.class, Fixtures.BrokenSetUpFixture.class);
        } finally {
            context.stop();
            Fixtures.logger = null;
        }
        writeTotals(file);

        List<String> summary = lines(Path.of(file + ".summary"));
        List<String> compact = lines(Path.of(file + ".compact"));
        List<String> full = lines(Path.of(file + ".full"));
        String text = String.join("\n", summary);

        assertThat(summary.get(0)).matches("---- JVM pid \\d+ in .*, logs .*test\\.compact: 9 tests, 5 failed, 1 aborted, 1 skipped, 2 not run, "
                + "1 failed test classes \\(set-up or tear-down\\) ----");

        // the exception, where it was thrown in the test and the command printing the log lines of the test
        String failed = token(summary, "Fixtures$MixedFixture.b_logsAndFails()");
        int entry = summary.indexOf("FAILED Fixtures$MixedFixture.b_logsAndFails() #" + failed);
        assertThat(summary.get(entry + 1)).isEqualTo("  org.opentest4j.AssertionFailedError: expected: <404> but was: <500>");
        assertThat(summary.get(entry + 2)).matches("  at .*Fixtures\\$MixedFixture\\.b_logsAndFails\\(Fixtures\\.java:\\d+\\)");
        // what the test logged at WARN and above, without running a command
        assertThat(summary.get(entry + 3)).isEqualTo("  WARN/ERROR logged during the test:");
        assertThat(summary.get(entry + 4)).matches("    WARN fixture #[0-9a-f]{12} : document service is slow");
        assertThat(summary.get(entry + 5)).isEqualTo("  logs: sed -n '/#" + failed + " start/,/#" + failed + " end/p' " + file + ".compact");
        assertThat(summary.get(entry + 6)).matches("  class logs \\(set-up, Spring context start\\): sed -n '/#t\\d+ start/,/#t\\d+ end/p' .*");

        assertThat(range(compact, failed)).containsExactly(
                "---- #" + failed + " start: Fixtures$MixedFixture.b_logsAndFails() ----",
                "INFO #" + failed + " loading document",
                "WARN #" + failed + " document service is slow",
                "---- #" + failed + " end (FAILED): Fixtures$MixedFixture.b_logsAndFails() ----").inOrder();
        assertThat(range(full, failed)).isEqualTo(range(compact, failed));

        // a message on one line, and the root cause
        assertThat(text).contains("  java.lang.IllegalStateException: outer second line\n");
        assertThat(text).contains("  root cause: java.lang.IllegalArgumentException: the root\n");
        // a failed test which logged nothing still has its markers
        assertThat(range(compact, token(summary, "Fixtures$MixedFixture.d_failsWithoutLogging()"))).hasSize(2);

        // invocation of a parameterized test and display name
        assertThat(text).contains("FAILED Fixtures$MixedFixture.g_parameterized() [[2] ");
        assertThat(text).doesNotContain("g_parameterized() [[1] ");
        token(summary, "Fixtures$MixedFixture.h_displayName() [returns not found]");
        // more failed tests than are written in detail: all are listed, and the same failure in another test is not repeated in detail
        assertThat(summary.get(1)).isEqualTo("Failed tests (6), the 5 marked * in detail below:");
        assertThat(summary.subList(2, 8).stream().filter(line -> line.startsWith("* ")).count()).isEqualTo(5);
        assertThat(summary.subList(2, 8).stream().filter(line -> line.startsWith("  ")).toList()).hasSize(1);
        assertThat(text).containsMatch("\n  #t\\d+ Fixtures\\$MixedFixture\\.i_sameFailure\\(\\): UnsupportedOperationException: not yet\n");
        assertThat(text).containsMatch("\n\\* #t\\d+ Fixtures\\$MixedFixture\\.b_logsAndFails\\(\\): AssertionFailedError: expected: <404> but was: <500>\n");
        assertThat(text).doesNotContain("FAILED Fixtures$MixedFixture.i_sameFailure()");
        // a failed set-up: the class has the entry, with its own log lines
        String setUp = token(summary, "Fixtures$BrokenSetUpFixture (test class set-up or tear-down)");
        assertThat(text).contains("FAILED Fixtures$BrokenSetUpFixture (test class set-up or tear-down) #" + setUp + ", 2 tests not run\n"
                + "  java.lang.IllegalStateException: no database\n"
                + "  at ");
        assertThat(text).containsMatch("  WARN/ERROR logged during the set-up or tear-down:\n    ERROR fixture #[0-9a-f]{12} : unable to start\n");
        assertThat(range(compact, setUp)).containsExactly(
                "---- #" + setUp + " start: Fixtures$BrokenSetUpFixture ----",
                "ERROR #" + setUp + " unable to start",
                "---- #" + setUp + " end (FAILED): Fixtures$BrokenSetUpFixture ----").inOrder();

        // compact file: nothing for a test which passed without logging; the full file has all markers
        assertThat(String.join("\n", compact)).doesNotContain("a_passesWithoutLogging");
        assertThat(String.join("\n", full)).contains("end (passed): Fixtures$MixedFixture.a_passesWithoutLogging() ----");
        assertThat(String.join("\n", full)).contains("end (aborted): Fixtures$MixedFixture.f_aborted() ----");

        // repeated warnings are collapsed in the compact file, also across tests; the summary has those of the failed test
        assertThat(compact.stream().filter(line -> line.matches("WARN #t\\d+ document service is slow")).count()).isEqualTo(1);
        assertThat(String.join("\n", compact)).contains("repeated 2 more times, 3 in total");

        // logging assertions (LogbackTestExtension) do not see the markers
        assertThat(observed.stream().map(ILoggingEvent::getFormattedMessage).filter(message -> message.startsWith("----")).toList()).isEmpty();
        assertThat(observed).hasSize(5);
    }

    @Test
    public void testsRunningAtTheSameTimeAreToldApartByTheirThread(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("test");
        LoggerContext context = logging(file, new ArrayList<>());
        Fixtures.logger = context.getLogger("fixture");
        try {
            run(new AiTestExecutionListener(true, file.toString(), file.toString(), () -> true, () -> context), Fixtures.ParallelFixture.class);
        } finally {
            context.stop();
            Fixtures.logger = null;
        }
        writeTotals(file);

        List<String> summary = lines(Path.of(file + ".summary"));
        List<String> compact = lines(Path.of(file + ".compact"));
        String failed = token(summary, "Fixtures$ParallelFixture.fails()");

        // the lines between the markers of the failed test include those of the other test, so the entry greps for the token instead
        assertThat(range(compact, failed).stream().anyMatch(line -> line.endsWith("the other test"))).isTrue();
        assertThat(String.join("\n", summary)).contains("  logs (other tests ran at the same time: the lines of the test's thread, and of other threads (~) which may be its own): grep '#"
                + failed + " ' " + file + ".compact\n");
        assertThat(compact.stream().filter(line -> line.contains("#" + failed + " ")).toList()).containsExactly(
                "---- #" + failed + " start: Fixtures$ParallelFixture.fails() ----",
                "WARN #" + failed + " the failing test",
                compact.stream().filter(line -> line.endsWith("from another thread")).findFirst().orElseThrow(),
                "---- #" + failed + " end (FAILED): Fixtures$ParallelFixture.fails() ----").inOrder();
        // the events of the test, and of another thread while both tests ran
        String text = String.join("\n", summary);
        assertThat(text).contains("  WARN/ERROR logged during the test (~: by another thread while several tests ran, may belong to another test):\n");
        assertThat(text).containsMatch("    WARN fixture #[0-9a-f]{12} : the failing test\n    ~ WARN fixture #[0-9a-f]{12} : from another thread\n");
        assertThat(text).doesNotContain("the other test");
        // one class: its lines are those between its markers
        assertThat(String.join("\n", summary)).containsMatch("  class logs \\(set-up, Spring context start\\): sed -n '/#t\\d+ start/,/#t\\d+ end/p' ");
        assertThat(compact.stream().filter(line -> line.endsWith("from another thread")).findFirst().orElseThrow()).matches("WARN ~ #t\\d+ #t\\d+ from another thread");
    }

    @Test
    public void eventsSharedByTheFailedTestsComeFirst(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("test");
        LoggerContext context = logging(file, new ArrayList<>());
        Fixtures.logger = context.getLogger("fixture");
        try {
            run(new AiTestExecutionListener(true, file.toString(), file.toString(), () -> true, () -> context), Fixtures.SharedCauseFixture.class);
        } finally {
            context.stop();
            Fixtures.logger = null;
        }
        writeTotals(file);

        List<String> summary = lines(Path.of(file + ".summary"));
        assertThat(summary.get(0)).matches("---- JVM pid \\d+ in .*: 9 tests, 7 failed, 0 aborted, 0 skipped ----");
        // logged during every failed test and no test which passed; the warning which every test logs is not listed
        assertThat(summary.get(1)).isEqualTo("WARN/ERROR events logged during several failed tests, likely causes first (one event of each kind):");
        assertThat(summary.get(2)).matches("  7 of 7 failed, 0 of 2 passed tests: WARN fixture #[0-9a-f]{12} : Unable to store document \\d \\| "
                + "java.lang.IllegalStateException: no connection \\| at .*Fixtures\\$SharedCauseFixture\\.store\\(Fixtures\\.java:\\d+\\)");
        assertThat(summary.get(3)).isEqualTo("Failed tests (7), the 5 marked * in detail below:");
        assertThat(summary.subList(4, 11).stream().filter(line -> line.startsWith("* #t")).count()).isEqualTo(5);
        assertThat(summary.subList(4, 11).stream().filter(line -> line.startsWith("  #t")).count()).isEqualTo(2);
        assertThat(summary.stream().filter(line -> line.startsWith("FAILED ")).count()).isEqualTo(5);
        assertThat(summary.stream().filter(line -> line.contains("passed tests: ")).count()).isEqualTo(1);
    }

    @Test
    public void summaryOfAPassingRunIsOneLine(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("test");
        // logging does not go to the AI files (for example a logback-test.xml of the project): no markers, but still a summary
        run(new AiTestExecutionListener(true, file.toString(), file.toString(), () -> false, LoggerContext::new), Fixtures.PassingFixture.class);
        writeTotals(file);

        List<String> summary = lines(Path.of(file + ".summary"));
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0)).matches("---- JVM pid \\d+ in [^,]*: 1 tests, 0 failed, 0 aborted, 0 skipped ----");
    }

    @Test
    public void failuresWithoutAiFilesHaveNoLogCommands(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("test");
        Fixtures.logger = new LoggerContext().getLogger("fixture");
        try {
            run(new AiTestExecutionListener(true, file.toString(), file.toString(), () -> false, LoggerContext::new), Fixtures.MixedFixture.class);
        } finally {
            Fixtures.logger = null;
        }
        writeTotals(file);

        String text = String.join("\n", lines(Path.of(file + ".summary")));
        assertThat(text).contains("FAILED Fixtures$MixedFixture.b_logsAndFails() #");
        assertThat(text).doesNotContain("sed -n");
    }

    @Test
    public void doesNothingUnlessEnabled(@TempDir Path directory) {
        Path file = directory.resolve("test");
        run(new AiTestExecutionListener(false, file.toString(), file.toString(), () -> true, LoggerContext::new), Fixtures.PassingFixture.class);
        writeTotals(file);

        assertThat(Files.exists(Path.of(file + ".summary"))).isFalse();
    }
}
