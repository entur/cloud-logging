# stderr-spring-boot-autoconfigure
Spring Boot auto-configuration that redirects `System.err` output to SLF4J.

## Features

 * Intercepts all output written to `System.err` and forwards it to an SLF4J logger
 * Detects `Throwable.printStackTrace` output and emits the full stack trace as a single log statement (rather than one log event per line)
 * Thread-safe: concurrent `printStackTrace` calls from different threads are accumulated independently
 * Restores the original `System.err` when the Spring application context is closed

## Configuration

| Property | Default | Description |
|---|---|---|
| `entur.logging.stderr.enabled` | `true` | Whether to forward `System.err` output to SLF4J |
| `entur.logging.stderr.level` | `error` | SLF4J log level (`trace`, `debug`, `info`, `warn`, `error`) |
| `entur.logging.stderr.logger` | `stderr` | Logger name used for forwarded output |

Disable with:

```yaml
entur:
  logging:
    stderr:
      enabled: false
```
