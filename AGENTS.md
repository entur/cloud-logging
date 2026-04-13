# Agents

## Project Overview

`cloud-logging` is a multi-module Gradle project providing JVM logging libraries for GCP and Azure, built on SLF4J 2, Logback, and Logback Logstash. It is **not** a generic logging library — it is specifically designed to produce structured JSON logs consumed by GCP Cloud Logging (via Fluentbit) and Azure Monitor.

## Build & Test

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Build a single module (example)
./gradlew :gcp:spring-boot-starter-gcp-web:build

# Check for dependency updates
./gradlew dependencyUpdates
```

## Module Structure

- `api/` – SLF4J `Logger`-wrapper (`DevOpsLogger`) with additional severity levels: `errorTellMeTomorrow`, `errorInterruptMyDinner`, `errorWakeMeUpRightNow`
- `appender/` – Core Logback async appender with MDC support and on-demand logging scope
- `gcp/` – GCP-specific Spring Boot starters, autoconfiguration, and Stackdriver/Cloud Logging encoder
- `azure/` – Azure-specific Spring Boot starters, autoconfiguration, and Azure Monitor encoder
- `request-response/` – Logbook-based HTTP and gRPC request/response logging
- `on-demand/` – Selective on-demand logging: caches log statements per request and only emits them for failed/slow/flagged requests
- `trace/` – Correlation-id and MDC tracing for HTTP and gRPC
- `micrometer/` – Log severity metrics (GCP and Azure variants)
- `test/` – Test utilities: Logback test configuration, JUnit 5 extensions
- `examples/` – Runnable Spring Boot example applications (web and gRPC, GCP and Azure)
- `bom/` – Bill of Materials for consumer dependency management

The `gcp/` and `azure/` modules are **mirrors** of each other in structure and feature set. When adding a feature to one, add the equivalent to the other.

## Key Versions

Dependency versions are managed centrally in the root `build.gradle` `ext` block. The Spring Boot BOM controls most transitive dependency versions (JUnit, Mockito, SLF4J, Logback, Jackson, Micrometer), and non-BOM dependency versions (e.g. `logbackLogstashVersion`, `logbookVersion`) are defined explicitly there. **Do not hardcode dependency versions in submodule `build.gradle` files** unless there is an intentional, documented exception; some example subprojects may declare plugin versions locally.

## Conventions

- Java with Spring Boot 4.x (not 3.x)
- Gradle multi-project build; module names declared in `settings.gradle`
- Tests use JUnit 5 and Google Truth (`com.google.truth`) assertions — not AssertJ or Hamcrest
- Machine-readable single-line JSON is the production log format; human-readable formats are test-scope only
- Test artifacts (e.g. `spring-boot-starter-gcp-web-test`) **must always be added alongside their production counterpart** — they configure human-readable output for local development
- **Never add a `logback.xml` or `logback-spring.xml` manually** — the starters provide these via classpath resources; a second file causes undefined behaviour
- Log levels are configured via Spring properties (`logging.level.root`, `logging.level.<package>`)

## Log Format and Cloud Platform Mapping

### GCP (Google Cloud Logging / Stackdriver)

Logs are written as JSON to stdout/stderr. Fluentbit on GKE forwards them to Cloud Logging as `jsonPayload`. The encoder (`StackdriverLogstashEncoder`) maps:

- SLF4J level → `severity` field (not `level`) — recognized by Cloud Logging for log severity filtering
- `message` field — includes stack traces inline
- MDC keys → top-level JSON fields (or OpenTelemetry trace fields `logging.googleapis.com/trace`, `logging.googleapis.com/spanId` if OTel is on classpath)
- `serviceContext.service` — populated from `HOSTNAME` environment variable

**Important GCP constraints (Fluentbit quirks):**
- Duplicate field names in `jsonPayload` cause Fluentbit to drop 2–4 seconds of logs from all pods — avoid emitting the same key twice in one log event
- Log lines exceeding 256 KB are dropped — truncate large payloads
- Avoid using reserved field names (`message`, `timestamp`) as structured argument keys

External docs:
- [GCP Structured Logging](https://cloud.google.com/logging/docs/structured-logging)
- [GCP LogEntry reference](https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry)
- [GCP Log severity levels](https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#LogSeverity)
- [GCP Error Reporting log format](https://cloud.google.com/error-reporting/docs/formatting-error-messages)

### Azure (Azure Monitor / Application Insights)

Logs are written as JSON to stdout/stderr and ingested by the Azure Monitor agent. The encoder (`AzureLogstashEncoder`) adds a `serviceContext` field populated from the `HOSTNAME` environment variable.

External docs:
- [Azure Monitor Logs overview](https://learn.microsoft.com/en-us/azure/azure-monitor/logs/data-platform-logs)
- [Azure Monitor log standard columns](https://learn.microsoft.com/en-us/azure/azure-monitor/logs/log-standard-columns)
- [Application Insights structured logging](https://learn.microsoft.com/en-us/azure/azure-monitor/app/structured-logging)

## On-Demand Logging

On-demand logging is **disabled by default**. It must be explicitly enabled:

```
entur.logging.http.ondemand.enabled=true   # for web
entur.logging.grpc.ondemand.enabled=true   # for gRPC
```

It caches all log statements per request and either discards them (happy path) or emits them (failure/slow/debug path). Failure triggers are configurable: HTTP status code, log level, call duration, or a specific HTTP/gRPC header.

## Testing Patterns

- Always test against machine-readable JSON output under the hood, even when the console shows human-readable format during development
- Use `CompositeConsoleOutputControl` to toggle output mode within a test:
  ```java
  try (Closeable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
      // test code
  }
  ```
- JUnit 5 extensions and Logback test utilities are in `test/test-logback-junit`

## Adding a New Module

1. Create the module directory and `build.gradle`
2. Register it in `settings.gradle` under the appropriate group (`include 'gcp:my-new-module'`)
3. If platform-specific, add the equivalent module for the other cloud platform
4. Add it to `bom/build.gradle` if it is a consumer-facing artifact
