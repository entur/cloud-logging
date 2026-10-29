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

# AI / tooling: compact log file
Set the JVM system property `entur.logging.ai.enabled=true` to write all log events to a file instead of the console.
The file is given by `entur.logging.ai.file` (default: `entur-logging-ai.log` in the temp directory).
The format is compact plain text, one event per line, without timestamp, thread or colours. MDC entries (present during requests) are appended to the logger as `[key=value, …]`. Stack traces are
on the same line as the log message (separated by ` | `), root-cause-first, limited to 15 frames, with framework frames removed,
so that `grep` returns a whole trace. Only INFO and above is written; override with
`entur.logging.ai.level` (for example `DEBUG`). A second file, `<file>.full`, gets the same events with DEBUG and above, timestamps, thread names and full stack traces,
for drill-down when the compact file is not enough; its level is set with `entur.logging.ai.full.level` (default `DEBUG`) (for example `grep -n 'some message' /tmp/test.log.full`).

When `entur.logging.ai.enabled=true`, HTTP and gRPC request-response logging is also compact (`CompactLogbackLogstashSink` for HTTP, `CompactLogbackLogstashGrpcSink` for gRPC): a single line without pretty-printing or colours (`message | header: value; ... | body`). Bodies are not truncated by default; set `entur.logging.ai.body.max=2000` to cut longer bodies (a `...[truncated, N chars]` marker is appended).
Events are appended; delete both files first for a clean run.

```
./gradlew test -Dentur.logging.ai.enabled=true -Dentur.logging.ai.file=/tmp/test.log
```

In the GCP test autoconfiguration this is implemented by `logback/logback-ai.xml`, loaded from `logback-test.xml`.
The root `build.gradle` forwards these properties to the forked test JVMs. In Gradle projects forward them to the `test` task yourself (see below); Maven Surefire/Failsafe forward `-D` properties automatically.

## Paste into your project (AGENTS.md / CLAUDE.md)

Requires the `*-test` artifact of this library on the test classpath.

**Maven:** nothing to configure; Surefire/Failsafe pass `-D` user properties on to the forked test JVM.

**Gradle:** forward the system properties to the forked test JVMs:

```groovy
test {
    ['entur.logging.ai.enabled', 'entur.logging.ai.file', 'entur.logging.ai.level', 'entur.logging.ai.full.level', 'entur.logging.ai.body.max'].each { key ->
        if (System.getProperty(key) != null) {
            systemProperty key, System.getProperty(key)
        }
    }
}
```

Then add this to the instructions file read by your AI agent:

````markdown
## Running tests and reading logs

When running tests, enable compact log output to save context. Logs then go to a file instead of the console:

    rm -f /tmp/test.log /tmp/test.log.full
    # Gradle
    ./gradlew test -Dentur.logging.ai.enabled=true -Dentur.logging.ai.file=/tmp/test.log
    # Maven
    mvn -q test -Dentur.logging.ai.enabled=true -Dentur.logging.ai.file=/tmp/test.log

- Console output only contains the test summary and failures. Do not use `-i`/`--info` (Gradle) or `-X` (Maven); they are not needed.
- `/tmp/test.log`: compact, one event per line (`LEVEL logger [mdc] : message`), INFO and above. Stack traces are on the same line as the
  message, root cause first, framework frames removed. Use `grep` (for example `grep -n 'ERROR\|WARN' /tmp/test.log`), do not read the whole file.
- `/tmp/test.log.full`: same events with timestamps, thread names, DEBUG and above and full stack traces.
  Only consult it (with `grep -n -C 5`) when the compact file is not enough.
- Include `-Dentur.logging.ai.level=DEBUG` to also get DEBUG in the compact file (`-Dentur.logging.ai.full.level=INFO` limits the `.full` file).
- Request/response logging (HTTP, gRPC) is a single line: `message | header: value; ... | body`.
````
