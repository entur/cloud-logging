# Plan: tie AI-mode logs to the failing tests (`test.failures`)

Plan on branch `ai-mode-test-failures`. Benchmark kit: abt-referencedata, branch `ai-mode-benchmark`, directory `ai-benchmark/` (see its README;
`ai-benchmark/setup.sh <version>` builds the bug copies there).

## Context

- AI mode (PR #535, branch `ai-mode-compact-test-logging`): with `-Dentur.logging.ai.enabled=true`, test logging goes to
  `<file>.compact` (one line per event) and `<file>.full` instead of the console. Agents get instructions from the AGENTS.md /
  CLAUDE.md snippet in `test/test-logback/README.md` ("Paste into your project").
- Classes in `test/test-logback`: package `no.entur.logging.cloud.logback.logstash.test.ai` (control, files, shutdown:
  `AiLoggingControl`, `AiFileAppender`, `AiShutdownHook`, `AiBodyMarker`, `FanOutAppender`, `ForwardingAppender`), `ai.common`
  (`AiEventDetails`, `AiAliases`, `AiEventId`, `AiText`, ...), `ai.compact` (`DeduplicatingAppender`, `CompactSettings`, converters),
  `ai.full`. Dependencies only point upwards; compact and full do not use each other. Wiring: `logback/logback-ai.xml` in
  `gcp/spring-boot-autoconfigure-gcp-test` and `azure/spring-boot-autoconfigure-azure-test` (keep both identical apart from the encoder).
- Related PRs: #538 (`ai-mode-benchmark`, log-question benchmark), #541 (draft: per-logger summary at the end of the test command),
  #542 (draft: 19 % shorter snippet, no measured loss).

## Problem (measured, Copilot CLI, Claude Sonnet 5.5, medium effort, bug-fixing in abt-referencedata)

- When the cause of a failing test is only in the logs (a swallowed exception logged as WARN), AI mode saves tokens only if the
  agent's first look shows the line: the agents always run `grep -n 'ERROR\|WARN' <logs>/test.compact | head -20`. With parallel
  tests the order of the ~41 WARN/ERROR lines varies, so whether the cause is in the first 20 is luck: 200-250k tokens when it is,
  600k-1.1M when it is not (then they read code like agents without AI mode, ~730k).
- Instructions do not fix it: "list all of them, without `head`" was ignored (they still add `head -20`), and a per-logger summary
  appended to the test command was dropped by 11 of 12 agents (they shorten long commands). The `#id` drill-down into `.full` is
  rarely used (only by Claude Code).
- So information has to come in a form the agents read anyway and that `head -20` cannot cut: a small file that answers
  "what was logged while my test failed".

## Goal

At the end of each test JVM in AI mode, write `<file>.failures`: per failed test, its name, the first line of its assertion,
and the WARN/ERROR events logged while it ran (compact format with `#id`). The instructions tell the agent to read it first.

```
3 tests failed in this JVM (abt-referencedata). WARN/ERROR events logged while they ran:

TimetableBatchParserTest > testProcessArchiveAllTypes: expected: <10> but was: <0>
  WARN TimetableBatchParser #20c1bea70175 : Got exception while trying to parse Timetable data file: _FLB_shared_data.xml | java.lang.IllegalArgumentException: Stop assignment refers to NSR:Quay:878, expected a StopPlace | at ...NetexParserUtil.requireRefOfType(NetexParserUtil.java:158)
TimetableBatchParserTest > testJourneyPatternRefsResolve...: Expected at least one ServiceJourney with a journeyPatternRef
  (the same events as above)
```

## Design

1. **Listener:** a JUnit Platform `TestExecutionListener` in `test-logback` (package `ai`), registered in
   `META-INF/services/org.junit.platform.launcher.TestExecutionListener`, active only when `AiLoggingControl.isAiEnabled()`.
   - Depend on `junit-platform-launcher` as `compileOnly`: Gradle and Maven provide it at test runtime. Check that a missing
     launcher never breaks a project (the service file is then never read).
   - `executionStarted`/`executionFinished`: record start/end time, class, display name, thread, result and the throwable's first line.
   - Check on which thread the callbacks run with Jupiter (parallel and sequential); if on the test thread, put the test name into
     the MDC there (key e.g. `test`) and remove it on finish. Leave that key out of the compact/full rendering, or show it as a short alias.
   - `testPlanExecutionFinished`: write the file (logging still runs then). Also write it from `AiShutdownHook` if the plan never finished.
2. **Events:** a small appender in `ai` (e.g. `FailureEventsAppender`), attached to `AI_FANOUT` in both `logback-ai.xml`, keeps
   WARN/ERROR events in memory (bounded, e.g. 10,000): time, thread, MDC test name, `AiEventId.idOf`, and the compact rendering
   (reuse the compact converters, so the line is identical to the one in `.compact`). It must see events before
   `DeduplicatingAppender` collapses them.
3. **Attribution:** events with the test's MDC name belong to it for certain. Other events (thread pools, Spring/gRPC/HTTP server
   threads) by time: from the start of the test class (covers `@BeforeAll` and Spring context start) to the end of the test.
   When other tests overlapped, say so: "(N other tests ran at the same time)".
4. **File format:** header with the number of failed tests and the module dir; per test the name, assertion line, events
   (cut like the compact file). Tests with the same events as the previous one: "(the same events as above)". At most ~10 events
   per test, then "and N more: grep ... <file>.compact". No failed tests: one line saying so, plus the WARN/ERROR count per
   logger (the summary from #541), so the file is useful for questions about warnings too. Several JVMs append sections, each
   with its own header (the lock file already serialises writes per run). Owner-only, like the other AI files.
5. **Instructions** (README snippet): a numbered procedure instead of facts: run the tests (unchanged command); if tests failed,
   `cat <logs>/test.failures` (read it whole); for a suspicious message, grep its logger in `.compact`; for a whole event,
   `grep -n -C 5 '#<id>' <logs>/test.full`; only then read code. One prohibition with its reason: do not `head`/`tail` the
   WARN/ERROR lines of `.compact`, their order depends on parallel tests. Keep commands short (agents shorten long ones).

## Limits to state in the README

Parallel tests make time-based attribution a candidate list; a cause logged before the failing test (e.g. in another class that
started a shared Spring context) is not in its window ("no WARN/ERROR events while this test ran" then points to `.compact`).

## Steps

1. Work on this branch, `ai-mode-test-failures` (from `ai-mode-compact-test-logging`); later a draft PR stacked on #535. Remove this
   plan file before the PR is merged.
2. Listener + appender + file writer, unit tests (Truth assertions; sequential, parallel, other-thread events, cascades, limits,
   no failures, several JVMs). Check the thread of Jupiter's listener callbacks first.
3. Wire into both `logback-ai.xml`; README snippet (procedure) and the AI-mode section; property to turn it off
   (`entur.logging.ai.failures=false`).
4. Full build in a separate worktree (another Gradle build in the main checkout broke two builds with missing test-result files).
   Only push after the build passed.
5. Try it on the examples and on abt-referencedata with the hard2 bug (abt-referencedata branch `ai-mode-benchmark`, `ai-benchmark/`).

## Measurement (Copilot only; at most 2 runs at a time)

- Bug benchmark, medium and hard2 bugs, 6 runs each: current snippet vs failures file + procedure. Compare tokens, tool calls,
  and whether the agents read `test.failures` first and found the cause there. Medium is the interesting one: its cause is logged
  in the failing test's own class.
- Log questions (#538), Sonnet medium and GPT-6 Luna low, 3 runs each: no loss in scores. First update `run-agents.sh` on branch
  `ai-mode-benchmark` (cloud-logging): its `snippet()` still extracts the old `/tmp/test` bullets; the snippet now uses `<logs>`.
- Afterwards 2-3 Claude Code runs to see whether Claude follows the procedure the same way (uses the user's Claude quota).

## Earlier results for reference (tokens per run)

| Bug | Baseline | Current snippet | Note |
|---|---|---|---|
| medium (WARN only) | 484k | 213k (morning), 312k (control) | spread 165k-663k |
| hard2 (swallowed exception) | 733k | 250k (morning), 680k (control) | spread 196k-1.1M |
