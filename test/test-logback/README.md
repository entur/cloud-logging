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
With `-Dentur.logging.ai.enabled=true` the tests write their log events to files instead of the console, which saves an AI agent's
context: a compact file with one event per line, a full file with everything (for drill-down), and a summary of the failed tests with
the WARN/ERROR events logged during each and the command which prints their log lines. See [AI mode](AI-MODE.md) for the files, their
format and the configuration.

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
- `<logs>/test.<pid>.compact` has one event per line (INFO and above, short stack traces), `<logs>/test.<pid>.full` everything (DEBUG,
  whole stack traces and bodies). Search them with `grep -n` instead of reading them; `grep -n '#id' <logs>/*.full` prints the whole
  event of an `#id` from the summary or the compact file.
````

**3. The permission rule** (Claude Code), in `.claude/settings.json`:

```json
{
  "permissions": {
    "allow": ["Bash(scripts/test-ai:*)"]
  }
}
```
