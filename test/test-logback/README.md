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

The files are `<file>.compact` and `<file>.full`, where `<file>` is given by `entur.logging.ai.file` (default: `entur-logging-ai` in the temp directory). Events are appended; delete both files first for a clean run.
The files hold whole request and response bodies, so they are created readable by the owner only (where the file system has POSIX permissions), and a file which is a symbolic link or belongs to another user is not written to.
All test JVMs (one per module) append to the same files; each JVM starts with a line `---- JVM pid N started <time> in <module dir> (aliases u1, h1, ... restart) ----`.
If test JVMs run at the same time (Gradle `--parallel` or `maxParallelForks`, Maven `forkCount`), their events are interleaved: only the JVM holding the lock file `<file>.lock` uses aliases, the others write
`(another JVM is writing to this file, no aliases)` in their line and leave UUIDs as they are. A JVM which cannot lock the file does not use aliases either (`(could not lock ..., no aliases)`). So an alias always belongs to the last line before it which says `aliases ... restart`.

### Compact file (`<file>.compact`)
One event per line, `LEVEL Logger [mdc] #id : message | fields: {...} | stack trace`, INFO and above (override with `entur.logging.ai.level`, for example `DEBUG`).

- No timestamp, thread or colours. The logger is the simple class name (or the full name if the logger is not named after a class). Errors logged with a `DevOpsLogger` severity show it after the level, `ERROR(WAKE_ME_UP_RIGHT_NOW)`, `ERROR(INTERRUPT_MY_DINNER)` or `ERROR(TELL_ME_TOMORROW)` (in both files).
- Messages longer than 4096 characters are cut (`...[truncated, N chars]`, with the `#id`), as are exception messages in stack traces; change the limit with `entur.logging.ai.message.max-size=N` (`0` = unlimited). Control characters are escaped (`\u001b`), also in stack traces and in both files, so that `grep` treats the files as text.
- MDC entries (present during requests) are appended to the logger as `[key=value, ...]`. The `logging.googleapis.com/` key prefix is removed, and such entries are left out when their value equals another entry's value (by default the trace is the correlation id).
- Structured fields which are not part of the message, from Logstash markers (`Markers.append("key", value)`, ...), SLF4J key-value pairs and structured arguments without a `{}` placeholder (`kv("key", value)`), are appended as JSON: ` | fields: {"key":value}` (in both files; cut like request-response bodies in the compact file).
- Stack traces are on the same line as the message (separated by ` | `), root cause first, with framework frames removed, so that `grep` returns a whole trace.
  `entur.logging.ai.stacktrace.max-lines` keeps the first 2 frames of each exception by default (`0` = unlimited) and `entur.logging.ai.stacktrace.max-causes=3` keeps the root cause and the next two wrapping exceptions (`[... N frames omitted]` / `[... more causes omitted]` markers; suppressed exceptions are shown but not counted).
- Events below WARN from framework loggers which only report start-up and shutdown (Tomcat, Spring test context, gRPC server, Hikari, ...) are left out, except the line with the port a server listens on (`Tomcat started on port N`). Override the logger name prefixes with `entur.logging.ai.quiet-loggers=a.b,c.d` (empty value: keep everything).
- Repeated WARN/ERROR events (same level and `DevOpsLogger` severity, logger, message and root cause) are collapsed: the first is printed, the rest are counted and a line `repeated N more times, N+1 in total (first #id, last #id): ...` is written (the total includes the first event) when the cache (500 keys, least recently used evicted) overflows or the test JVM stops. Disable with `entur.logging.ai.dedupe=false`.

### Full file (`<file>.full`)
The same events with timestamps, thread names, full logger names, DEBUG and above (`entur.logging.ai.full.level`, default `DEBUG`), all request-response headers, whole bodies, the other request-response details (` | fields: {"http":{"origin":...,"protocol":...,"remote":...}}`) and full stack traces, for drill-down when the compact file is not enough.
Every event has an id (`#1a2b3c4d5e6f`) here. In the compact file, only events which lost information (stack trace, truncated message, body or fields) show the id, so `grep -n -C 5 '#1a2b3c4d5e6f' <file>.full` gives the complete event.

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

Then add this to the instructions file read by your AI agent. If the file already has a test command (for example with a profile such as
`-Pquick`), add the `-D` options and the log directory to that command instead of using the generic one below, and remove rules which
contradict it (such as a rule against `-q`).

Each test run gets its own log directory in `/tmp`, so test runs of other projects, or of other checkouts of the same project, never write to the
same files, also when they run at the same time (the test JVMs of one run share the files). The directories stay until `/tmp` is cleaned,
usually at reboot.

````markdown
## Running tests and reading logs

When running tests, enable compact log output to save context. Logs then go to files in a new directory instead of the console.
Run each command as one line (it creates the directory and uses it):

    # Gradle (--rerun: also if the tests are up to date)
    L=$(mktemp -d "/tmp/$(basename "$PWD")-tests.XXXXXX"); echo "logs: $L"; ./gradlew test --rerun -q --console=plain -Dentur.logging.ai.enabled=true -Dentur.logging.ai.file=$L/test > $L/test.out 2>&1; echo "exit code $?"; head -40 $L/test.out
    # Maven (keep only the Maven/test error lines, not the plugin and JVM warnings or client stack traces)
    L=$(mktemp -d "/tmp/$(basename "$PWD")-tests.XXXXXX"); echo "logs: $L"; mvn -q test -Dentur.logging.ai.enabled=true -Dentur.logging.ai.file=$L/test > $L/test.out 2>&1; echo "exit code $?"; grep '^\[ERROR\]' $L/test.out | cut -c1-300 | head -30

- `<logs>` below is the directory the command printed (`logs: /tmp/myproject-tests.Ab12Cd`); use it in later commands, as variables do not
  survive from one command to the next. Every run gets a new one, so nothing has to be deleted first.
- Console output (`<logs>/test.out`) only contains the test summary and failures (filtered as above, usually a few KB); a non-zero exit code means
  failed tests or a broken build. With `-q`, Maven prints no test summary: the counts per test class are in `target/surefire-reports/*.txt`
  (files of test classes which did not run this time are left from earlier runs). Do not use `-i`/`--info` (Gradle) or `-X` (Maven); they are not needed.
- `<logs>/test.compact`: compact, one event per line (`LEVEL SimpleClassName [mdc] #id : message`), INFO and above. Stack traces are on the same line as the
  message, root cause first, framework frames removed, 2 frames per exception. Use `grep` (for example `grep -n 'ERROR\|WARN' <logs>/test.compact`), do not read the whole file.
- `<logs>/test.full`: the same events plus what the compact file leaves out: DEBUG, timestamps, thread names, full logger names,
  framework start-up/shutdown lines (server ports are kept in the compact file), transport headers (`user-agent`, `accept*`, `grpc-*encoding`, `date`, ...),
  whole bodies and full stack traces. Some things are only logged at DEBUG and so only appear here, for example requests rejected by
  a security filter before request-response logging (`grep -n 'Response 4[0-9][0-9]\|Rejecting access' <logs>/test.full`). Look for those directly in this file with `grep -n`; an `#id` in the compact file (truncated stack trace, message, body or fields)
  finds the complete event with `grep -n -C 5 '#id' <logs>/test.full`.
- Repeated WARN/ERROR events are collapsed in `<logs>/test.compact`: the first is printed, the rest are summarized later in a line
  `repeated N more times, N+1 in total (first #id, last #id): message` whose total includes that first event. Counting restarts when logging
  is reconfigured (e.g. a new Spring context), so a message can have several such lines. For exact counts grep `<logs>/test.full` for the
  logger and the message, as the same message can come from several loggers: `grep -c 'DocumentEndpoint .* : Hello world' <logs>/test.full`.
- UUIDs and hex ids are replaced by aliases (`u1`, `h1`, ...) in both files; grep `<logs>/test.full` for a raw id to find its alias, `u7(<uuid>)`.
- For the exact JSON the servers log, run without AI mode and with `-Dentur.logging.style=machineReadableJson`; with Gradle,
  `sed -n 's/^  <system-out><!\[CDATA\[{/{/; /^{"/p' build/test-results/test/*.xml` prints it.
````
