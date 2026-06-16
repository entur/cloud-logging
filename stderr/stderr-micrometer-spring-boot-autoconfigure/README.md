# stderr-micrometer-spring-boot-autoconfigure
Spring Boot auto-configuration that intercepts `System.err` output and increments Micrometer counters.

## Features

 * Intercepts all output written to `System.err` and increments Micrometer error counters on each newline
 * Uses the same `logback.events` metric name and tags as `DevOpsMetricsTurboFilter` so the counts are additive with regular log-level metrics
 * Restores the original `System.err` when the Spring application context is closed

Note: This module assumes use of FunctionCounter meters as registered by spring-boot-autoconfigure-azure or spring-boot-autoconfigure-gcp.

## Configuration

| Property | Default | Description |
|---|---|---|
| `entur.logging.stderr.micrometer.enabled` | `true` | Whether to intercept `System.err` output and update Micrometer counters |

Disable with:

```yaml
entur:
  logging:
    stderr:
      micrometer:
        enabled: false
```
