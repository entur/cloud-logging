# logback test
Composite appender with room for three encoders.

# human-readable (for local development)
The first two encoders are for printing to console:

- 'classic' one-line logging
  - color coded
- 'friendly' JSON logging
  - pretty-printed,
  - color coded, and
  - with fewer default fields

# machine-readable (for unit testing)
The last encoder is for unit testing of logging, i.e. machine-readable JSON output.

## Known issues
Can not yet assert against MDC fields set in gRPC MDC context.

When using an async appender, there is a race condition for in-flight log events when switching from one encoder to another.

# Console output as on the servers
`-Dentur.logging.style=machineReadableJson` switches the console to the output of the servers: one JSON object per line, with the
encoder settings and request-response sinks used in production (no pretty-printing or colours). This shows what Cloud Logging or
Azure Monitor receive, for example to check the fields of a log statement:

```
./gradlew test --rerun -Dentur.logging.style=machineReadableJson
```

The other values are `humanReadablePlain` (the default) and `humanReadableJson`. It is the Spring property read by `LoggingStyleAutoConfiguration`,
so it can also be set in `application.properties` or with `@TestPropertySource`. On the command line, Gradle projects must forward it to the
`test` task (see [Paste into your project](#paste-into-your-project-agentsmd--claudemd)); Maven Surefire/Failsafe forward `-D` properties.

- It applies when the first Spring context of the test JVM starts, and from then on for the whole JVM. Lines logged before that (Spring Boot start-up,
  tests without a Spring context which run first) are human-readable.
- Code which switches the output (`CompositeConsoleOutputControl.useHumanReadableJsonEncoder()` and so on) still does so within its block.
- The test starters set a few framework loggers (Tomcat, Jetty, Hibernate Validator, ...) to WARN or ERROR, which the servers do not.
  `serviceContext` is filled from the `HOSTNAME` environment variable, so it is the local host name.
- On the console, Gradle only shows the lines logged after the last test (at JVM exit); with `testLogging.showStandardStreams` (and
  without `-q`) it shows all of them, but indented. The lines as written are in the test reports; this prints the JSON lines: `sed -n 's/^  <system-out><!\[CDATA\[{/{/; /^{"/p' build/test-results/test/*.xml`
- AI mode (`entur.logging.ai.enabled=true`) writes nothing to the console, so do not combine the two.

# AI / tooling: compact log file
Set the JVM system property `entur.logging.ai.enabled=true` (exactly `true`, in lower case) to write all log events to files instead of the console:

```
./gradlew test --rerun -Dentur.logging.ai.enabled=true -Dentur.logging.ai.file=/tmp/test
```

`--rerun` runs the tests even if Gradle considers them up to date (or cached), otherwise no logs are written.

The files are `<file>.<pid>.compact`, `<file>.<pid>.full` and `<file>.summary` (the failed tests, see [Summary file and test markers](#summary-file-and-test-markers)), where `<file>` is given by `entur.logging.ai.file` (default: `entur-logging-ai` in the temp directory). Events are appended; delete the files first for a clean run.
The files hold whole request and response bodies, so they are created readable by the owner only (where the file system has POSIX permissions), and a file which is a symbolic link or belongs to another user is not written to.
Every test JVM (one per module; more with Gradle `maxParallelForks` or Maven `forkCount`) writes its own compact and full file, with its pid in the name, so test JVMs running at the same time
do not write into each other's lines; a file starts with a line `---- JVM pid N started <time> in <module dir> ----`. The summary file is shared and names the files of each JVM.

### Compact file (`<file>.<pid>.compact`)
One event per line, `LEVEL Logger [mdc] #id : message | fields: {...} | stack trace`, INFO and above (override with `entur.logging.ai.level`, for example `DEBUG`).

- No timestamp, thread or colours. The logger is the simple class name (or the full name if the logger is not named after a class). Errors logged with a `DevOpsLogger` severity show it after the level, `ERROR(WAKE_ME_UP_RIGHT_NOW)`, `ERROR(INTERRUPT_MY_DINNER)` or `ERROR(TELL_ME_TOMORROW)` (in both files).
- Messages longer than 4096 characters are squeezed first (runs of spaces and tabs become one space, runs of more than 3 of the same character other than a letter or digit become 3, as in the borders and padding of a table), then cut if still too long (`...[truncated, N chars]`, with the `#id`), as are exception messages in stack traces; bodies are only cut; change the limit with `entur.logging.ai.message.max-size=N` (`0` = unlimited). Control characters are escaped (`\u001b`), also in stack traces and in both files, so that `grep` treats the files as text.
- MDC entries (present during requests) are appended to the logger as `[key=value, ...]`. The `logging.googleapis.com/` key prefix is removed, and such entries are left out when their value equals another entry's value (by default the trace is the correlation id).
- Structured fields which are not part of the message, from Logstash markers (`Markers.append("key", value)`, ...), SLF4J key-value pairs and structured arguments without a `{}` placeholder (`kv("key", value)`), are appended as JSON: ` | fields: {"key":value}` (in both files; cut like request-response bodies in the compact file).
- Stack traces are on the same line as the message (separated by ` | `), root cause first, with framework frames removed, so that `grep` returns a whole trace.
  `entur.logging.ai.stacktrace.max-lines` keeps the first 2 frames of each exception by default (`0` = unlimited) and `entur.logging.ai.stacktrace.max-causes=3` keeps the root cause and the next two wrapping exceptions (`[... N frames omitted]` / `[... more causes omitted]` markers; suppressed exceptions are shown but not counted).
- Events below WARN from framework loggers which only report start-up and shutdown (Tomcat, Spring test context, gRPC server, Hikari, ...) are left out, except the line with the port a server listens on (`Tomcat started on port N`). Override the logger name prefixes with `entur.logging.ai.quiet-loggers=a.b,c.d` (empty value: keep everything).
- Repeated WARN/ERROR events (same level and `DevOpsLogger` severity, logger, message and root cause) are collapsed: the first is printed, the rest are counted and a line `repeated N more times, N+1 in total (first #id, last #id): ...` is written (the total includes the first event) when the cache (500 keys, least recently used evicted) overflows or the test JVM stops. Disable with `entur.logging.ai.dedupe=false`.

### Full file (`<file>.<pid>.full`)
The same events with timestamps, thread names, full logger names, DEBUG and above (`entur.logging.ai.full.level`, default `DEBUG`), all request-response headers, whole bodies, the other request-response details (` | fields: {"http":{"origin":...,"protocol":...,"remote":...}}`) and whole stack traces (all frames and causes), for drill-down when the compact file is not enough.
Every event is one line here too: a message with line breaks is joined, and the stack trace follows the message, separated by ` | `, so `grep` returns a whole event.
Every event has an id (`#1a2b3c4d5e6f`) here. In the compact file, only events which lost information (stack trace, truncated message, body or fields) show the id, so `grep -n '#1a2b3c4d5e6f' <file>.<pid>.full` gives the complete event.

Some things are only logged at DEBUG, and so are only in the full file, for example requests rejected by a security filter before request-response logging (`grep -n 'Response 4[0-9][0-9]\|Rejecting access' <file>.*.full`).
To see the exact JSON the servers log (for example the fields of a log statement), run the tests without AI mode and with `-Dentur.logging.style=machineReadableJson`; with Gradle, print the JSON lines from the test reports:
`sed -n 's/^  <system-out><!\[CDATA\[{/{/; /^{"/p' build/test-results/test/*.xml | grep 'some message'` (Maven: in the test output). Lines logged before the first Spring context has started are not JSON.

### Summary file and test markers
A JUnit Platform `TestExecutionListener` (`AiTestExecutionListener`, registered through `META-INF/services`, so there is nothing to configure with
Gradle, Maven or an IDE) tells which tests failed and where their log lines are. It does nothing unless `entur.logging.ai.enabled=true`.

`<file>.summary` has a block per test JVM, written when the JVM exits: a line with the number of tests (the skipped ones included, as Maven Surefire and Gradle count them; the only line if nothing failed), the command which prints the log lines of a test or test class with its token, and an entry per failed test:

```
---- JVM pid 4711 in /work/app, logs /tmp/test.4711.compact: 118 tests, 1 failed, 0 aborted, 2 skipped ----
log lines: sed -n '/#tN start/,/#tN end/p' /tmp/test.4711.compact with the #tN of a test, or of its class (class #tN: set-up, Spring context start)
FAILED DocumentEndpointTest.returnsNotFound() #t17, class #t12
  org.opentest4j.AssertionFailedError: expected: <404> but was: <500>
  at no.entur.DocumentEndpointTest.returnsNotFound(DocumentEndpointTest.java:42)
  WARN/ERROR logged during the test:
    ERROR DocumentService #1a2b3c4d5e6f : Unable to load document 7 | java.io.IOException: timeout | at no.entur.DocumentStore.load(DocumentStore.java:88)
```

- An entry has the exception (and its root cause, if there is one), each with the stack frame which tells most: the first one in the project's own code (a class loaded from a
  directory of class files, not from a jar), else the first one which is not in a test framework, an assertion library or the JDK. A listed event has that frame of its root cause (`| at ...`) and, if the class which logged the event is further down in the stack trace,
  the frame of that class (`| called from ...`): an exception which is caught and logged is often thrown in code which only got a wrong value, and the fault is in its caller.
  A listed event without an exception has the frame of its log statement if that is in the project's own code (`| at ...`), and the first frame of the project's own code
  in another class (`| called from ...`): a warning about a wrong value is often logged by the class which checks it, and the fault is in the caller which passed the value.
  The name is `Class.method()`, followed by the display name in brackets if it tells more (the invocation of a parameterized test, `@DisplayName`).
- `WARN/ERROR logged during the test` lists what the test logged at WARN and above (up to 8 different events, repeats counted; message, root cause and its first frame
  outside frameworks, with the `#id` of the event in the full file), so that a cause which is only in the log is visible without running a command. It has the events
  logged on the thread of the test, and those of other threads while it ran; if several tests ran then, the latter are marked `~` and may belong to another test.
  Also events which the compact file collapses as repeated. If the test logged none, the events of the set-up of its class are listed (a Spring context which failed to start),
  except those which a test class that passed also logged during its set-up (framework warnings at every Spring context start): `... and 2 events which test classes that passed also logged during their set-up`.
- If the set-up or tear-down of a test class fails (`@BeforeAll`, ...), the class has the entry, with the number of its tests which did not run.
- Messages are on one line, squeezed like in the compact file if too long, and cut at 500 characters (logged events: 300) with `...[truncated, 812 chars]`; the `#id` of an event gives the whole event in the full file. UUIDs are not replaced by aliases here; see [Aliases](#aliases) for finding them in the log files.
- Tests which failed the same way have one entry, with a line `the same failure in 2 more tests: returnsGone() at line 51 #t18, OtherTest.returnsNothing() at OtherTest.java:30 #t25` (at most 10 named, then `and 3 more`):
  the same exception and message, thrown at the same place outside the test's own class (an assertion fails at another line in each test), and the same kinds of WARN/ERROR events
  (see below), so that tests which fail alike for different reasons have an entry each. The line of each named test is where its test method failed or called what failed,
  in the file of the frame shown above it unless another file is named; it is left out where it is that of the first test (the invocations of a parameterized test).

With many failed tests the block stays short, and what they have in common comes first:

```
---- JVM pid 4711 in /work/app, logs /tmp/test.4711.compact: 94 tests, 24 failed, 0 aborted, 0 skipped ----
log lines: sed -n '/#tN start/,/#tN end/p' /tmp/test.4711.compact with the #tN of a test, or of its class (class #tN: set-up, Spring context start)
WARN/ERROR events logged during several failed tests, likely causes first (one event of each kind):
  [1] 24 of 24 failed, 0 of 70 passed tests: WARN DocumentCache #1a2b3c4d5e6f : Unable to store document 7 | java.lang.IllegalArgumentException: no id | at no.entur.DocumentCache.put(DocumentCache.java:88)
  [2] 9 of 24 failed, 3 of 70 passed tests: WARN DocumentClient #2b3c4d5e6f1a : Retrying request 2
FAILED DocumentEndpointTest.returnsNotFound() #t17, class #t12
  org.opentest4j.AssertionFailedError: expected: <404> but was: <500>
  at no.entur.DocumentEndpointTest.returnsNotFound(DocumentEndpointTest.java:42)
  WARN/ERROR logged during the test: [1] #3c4d5e6f1a2b (above)
  the same failure in 8 more tests: returnsGone() at line 51 #t18, ...
...
Other failed tests (12), one line per failure:
  #t31 DocumentListTest.listsDocuments(): AssertionFailedError: expected: <3> but was: <0>, and the same in 2 more tests: ...
  ...
```

- `WARN/ERROR events logged during several failed tests`: the kinds of events which more than one failed test logged, with the number of failed and of passed tests which
  logged them, and one event of each kind. Events are of the same kind if they have the same logger and were thrown at the same place (root cause and its first frame outside
  frameworks) or, without an exception, have the same message apart from digits. Those logged by a larger share of the failed than of the passed tests are listed
  (at most 5, then `... and 2 more kinds`), the most specific to the failures first: an event of all failed tests and no passed test is likely the cause, one which most tests log is not listed.
  They are numbered, and an entry refers to an event of a listed kind by its number and the `#id` of its own event, `[1] #3c4d5e6f1a2b (3 times)`, instead of repeating it.
- 5 failures are written in detail; then the other failures, one line each with the token and the root cause (at most 30, then `... and 4 more failures, of 9 tests`).
  The log lines of any test are printed by the command of the block with its token.
- As the block is written at JVM exit, a test JVM which is killed leaves none; the build tool reports that.

The command works because both log files have a marker line at the start and at the end of every test and test class,
`---- #t17 start: DocumentEndpointTest.returnsNotFound() ----` and `---- #t17 end (FAILED): ... ----` (`passed`, `FAILED` or `aborted`);
the token is a counter, unique within the files of a test JVM. Replace `.compact` by `.full` in the command for everything logged during the test.

- The markers are queued with the log events, so they are in order with what the test thread logs. Events logged by other threads (a server handling a request of the test) are queued when they are written.
- The test instance is created before the test starts, and with it the Spring application context of the first test of a class. Those lines are in the range of the class, not of the test.
- In the compact file a test (or class) which passed without logging anything has no marker lines.
- An event logged on the thread of a test has the token of that test after the logger, `WARN DocumentService #t17 : message` (in both files; the token of the test class during its set-up).
  With tests running at the same time (JUnit parallel execution) the lines between the markers of a test also have the events of the others, so the entry of such a test has
  `logs (other tests ran at the same time: ...): grep '#t17 ' <file>.<pid>.compact` instead.
- An event logged on another thread (a server handling a request of the test, an executor) while several tests are running has the tokens of all of them after a `~`,
  `WARN TimetableParser ~ #t17 #t21 : message`: it belongs to one of them (or to none), and the `grep` of each of them prints it. While only one test is running such an event has
  no token; it is between the markers of that test.
- The test instance of a test (and a Spring context it starts) is created on its thread before the test has its token; with tests running at the same time those events have the
  token of the class only if it runs on the same thread. Running the one test again gives its events alone.
- Logging assertions (`LogbackTestExtension`, `LogStatements`) do not see the markers.
- If logging does not go to the AI files (a project's own `logback-test.xml`), the summary is still written, without the command.

### Request-response logging
HTTP and gRPC request-response logging uses the machine-readable sinks, so message and JSON fields are the same as without AI mode and logging assertions behave the same.
The files render each event on a single line without pretty-printing or colours, `message | header: value; ... | body: <body>`, with headers and body taken from the request-response marker
(HTTP: `AiLogLevelLogstashLogbackSink` / `AiOndemandLogLevelLogstashLogbackSink` also attach the whole body of JSON, XML, `text/*` and form requests and responses, which JSON output does not show).
As the files show the whole body, the message's note about the JSON output's body (`[truncated ~N KB]`) is left out.
JSON bodies and JSON header values (such as rich gRPC error details) only lose the whitespace between tokens; other bodies have line breaks replaced by spaces, and binary content is replaced by `[binary, N chars]`. In the compact file only:

- Transport headers (`user-agent`, `accept-encoding`, `grpc-encoding`, `date`, ...) and headers repeated in the MDC (same name, ignoring an `x-` prefix, case and punctuation, and the same value, for example `x-correlation-id` and `correlationId`) are left out. Override the header names with `entur.logging.ai.request-response.excluded-headers=a,b` (empty value: keep all headers).
- Bodies longer than 4096 characters are cut (a `...[truncated, N chars]` marker is appended); change the limit with `entur.logging.ai.request-response.max-body-size=N` (`0` = unlimited).

### Aliases
UUIDs in MDC values and in messages (including request-response headers and bodies) are replaced by short aliases `u1`, `u2`, ..., and hex ids in MDC values and request-response headers (such as span ids, or the ids in `traceparent`) by `h1`, `h2`, ..., in both files (consistent within one JVM, up to 10000 values; later new values are not aliased).
The first time an alias appears in the full file it is followed by its value, `u7(123e4567-e89b-12d3-a456-426614174000)`, so a value from elsewhere (for example a failed assertion) can be found with `grep`. Disable with `entur.logging.ai.alias=false`.

### Configuration
In the GCP and Azure test autoconfiguration this is implemented by `logback/logback-ai.xml`, loaded from `logback-test.xml` (with Spring Boot's default logger levels, as without AI mode).
A shutdown hook stops logging at JVM exit, so that queued events and `repeated` lines are written. When Spring Boot has configured logging, it first waits for Spring Boot's shutdown hook, which stops logging after closing the application contexts, and only stops logging itself if Spring Boot did not (for example with `logging.register-shutdown-hook=false`).
Request-response logging only switches to the AI-mode sinks when the AI files are actually written, so a project whose own `logback-test.xml` shadows the starter's keeps its usual console output.
The root `build.gradle` forwards all `entur.logging.*` properties to the forked test JVMs. In Gradle projects forward them to the `test` task yourself (see below); Maven Surefire/Failsafe forward `-D` properties automatically.

## Paste into your project (AGENTS.md / CLAUDE.md)

Requires the `*-test` artifact of this library on the test classpath.

**Maven:** nothing to configure; Surefire/Failsafe pass `-D` user properties on to the forked test JVM.

**Gradle:** forward all `entur.logging.*` system properties (the AI-mode properties and `entur.logging.style`) to the forked test JVMs (Gradle 7.5 or later):

```groovy
test {
    systemProperties providers.systemPropertiesPrefixedBy('entur.logging.').get()
}
```

Kotlin DSL: `tasks.test { systemProperties(providers.systemPropertiesPrefixedBy("entur.logging.").get()) }`.

Then add three things to your project: a test script, a few lines in the instructions file read by your AI agent, and (for Claude Code)
a permission rule.

**1. The script**, as `scripts/test-ai` (`chmod +x scripts/test-ai`). It works with Maven and Gradle. If your project already has a test
command (for example with a profile such as `-Pquick`), add those options to the Maven or Gradle line in it. Remove rules in the
instructions file which contradict it (such as a rule against `-q`).

```bash
#!/usr/bin/env bash
# Runs the tests with compact log output (cloud-logging AI mode): the logs go to files in a new directory in /tmp, and
# this prints that directory, the exit code and the test summary. Run it from the project root.
# Extra arguments go to Maven or Gradle:
#   Maven:  scripts/test-ai -Dtest=FooTest#bar          scripts/test-ai -pl app
#   Gradle: scripts/test-ai --tests FooTest             scripts/test-ai :app:test --tests FooTest
L=$(mktemp -d "/tmp/$(basename "$PWD")-tests.XXXXXX") || exit 1
echo "logs: $L"
AI=(-Dentur.logging.ai.enabled=true "-Dentur.logging.ai.file=$L/test")
if [ -x ./gradlew ]; then
  # the first argument may be the test task of one project (:app:test); --tests must match in every project it runs in
  task=test
  case ${1-} in [!-]*) task=$1; shift ;; esac
  # --rerun: also if the tests are up to date or cached
  ./gradlew "$task" --rerun -q --console=plain "${AI[@]}" "$@" > "$L/test.out" 2>&1
else
  mvn=mvn
  [ ! -x ./mvnw ] || mvn=./mvnw
  # no build cache (it would skip the tests); -Dtest needs no match in every module
  "$mvn" -q test -Dmaven.build.cache.enabled=false -Dsurefire.failIfNoSpecifiedTests=false "${AI[@]}" "$@" > "$L/test.out" 2>&1
fi
status=$?
echo "exit code $status"
if [ -s "$L/test.summary" ]; then
  head -60 "$L/test.summary"
  lines=$(wc -l < "$L/test.summary")
  if [ "$lines" -gt 60 ]; then
    echo "... $((lines - 60)) more lines in $L/test.summary"
    # the count lines of the test JVMs further down: those with failures in full, the others counted
    ok=': [0-9]+ tests, 0 failed, [0-9]+ aborted, [0-9]+ skipped ----$'
    failed=$(tail -n +61 "$L/test.summary" | grep '^---- JVM ' | grep -v -E "$ok")
    passed=$(tail -n +61 "$L/test.summary" | grep '^---- JVM ' | grep -c -E "$ok")
    n=$(printf '%s' "$failed" | grep -c '^')
    [ "$n$passed" = 00 ] || echo "test JVMs further down: $n with failures, $passed in which no test failed${failed:+:}"
    [ -z "$failed" ] || printf '%s\n' "$failed"
  fi
else
  echo "no test summary: no tests ran (or none matched), the build is broken, or AI mode is not on the test classpath. From $L/test.out:"
  grep -E -A2 '^\[ERROR\]|What went wrong|error:|^e: ' "$L/test.out" | grep -v '^--$' | cut -c1-300 | head -30 | grep . ||
    grep -v '^WARNING: ' "$L/test.out" | tail -20 | cut -c1-300
fi
exit $status
```

- **Exit code and summary, also on success:** the script prints the exit code and the summary (`573 tests, 0 failed, ...`) also when all
  tests pass. Agents which filter quiet Maven output themselves (`mvn -q test | tail`) see no result on success, and often run the whole
  suite again to be sure.
- **Never silent:** without a summary (no tests ran or none matched, a broken build, the `*-test` artifact missing) it says so and prints the
  errors from the build output.
- **Long summaries:** it prints the first 60 lines of the summary, then how many lines are left. Each test JVM appends its own block, and
  in a build with many modules the first blocks can fill those lines. So it then also prints the count line of each test JVM further down
  which had failures, and how many others there are: `test JVMs further down: 1 with failures, 12 in which no test failed:`.
- **No build cache:** with the Maven build cache extension, an unchanged module is restored from the cache and its tests do not run. Gradle
  has `--rerun` for the same reason.
- **One log directory per run:** each run gets its own directory in `/tmp`, so test runs of other projects, or of other checkouts of the same
  project, never write to the same files, also when they run at the same time (the test JVMs of one run share the directory). The
  directories stay until `/tmp` is cleaned, usually at reboot.
- **Why a script and not a one-line command:** Claude Code's permission rules cannot approve a command with `$(...)`, variables and
  redirects, such as this script written as one line ("Quoted text in this command can't be checked before it runs"), so every test run
  would need a prompt or auto mode. A script needs one rule.

**2. The instructions** (AGENTS.md / CLAUDE.md):

````markdown
## Running tests and reading logs

Run the tests with `scripts/test-ai` (compact log output, which saves context: the logs go to files in a new directory instead of the
console). Arguments go to Maven/Gradle, for example `scripts/test-ai -Dtest=FooTest#bar` (Maven) or `scripts/test-ai --tests FooTest` (Gradle).

- It prints the log directory (`logs: /tmp/...`, `<logs>` below), the exit code and `<logs>/test.summary`: per test JVM the number of
  tests, the command which prints the log lines of a test with its token (`#t17`; `.full` instead of `.compact` in it: everything), then
  each failed test with its exception and the WARN/ERROR events logged during it (often the cause). With many failed tests, the events
  which they have in common come first, numbered, and tests which failed the same way have one entry.
- `<logs>/test.<pid>.compact` has one event per line (`LEVEL Logger #t17 [mdc] #id : message | ...`, INFO and above, short stack
  traces), `<logs>/test.<pid>.full` everything (DEBUG, some events only there such as requests a security filter rejects, whole stack
  traces and bodies). Search them with `grep -n` instead of reading them; `grep -n '#id' <logs>/*.full` prints the whole event of an
  `#id` from the summary or the compact file.
- A repeated WARN/ERROR is one compact line, then `repeated N more times, N+1 in total` (the first included). Exact counts:
  `grep -c ' WARN .*MyService .* : Hello' <logs>/*.full` (level, logger, message; one count per file).
- An HTTP call is two lines with the same `requestId`: the request (`POST /api/x | ...`, the only one with the method) and the
  response (`404 Not Found /api/x (in 2 ms) | ...`).
````

**3. The permission rule** (Claude Code), in `.claude/settings.json`:

```json
{
  "permissions": {
    "allow": ["Bash(scripts/test-ai:*)"]
  }
}
```
