[![Maven Central](https://img.shields.io/maven-central/v/no.entur.logging.cloud/api.svg)](https://mvnrepository.com/artifact/no.entur.logging.cloud)

# cloud-logging
Logging libraries for JVM applications. Goals

 * make developers more productive when developing and maintaining applications
 * keep cloud logging costs within reason
 * help comply with privacy constraints for logging

# Why this library exists

Four capabilities are the core reason this is a library. Everything else — the Spring Boot
starters, the test-vs-production output switching, the extra error levels — is packaging and
ergonomics around them.

 1. **On-demand logging.** Every log statement for a request is buffered and discarded on
    success. If the request fails, is too slow, or is flagged (by status code, log level,
    call duration, or a troubleshooting header), the whole buffer is flushed — including the
    `INFO`/`DEBUG` siblings that give the failing statement its context. This keeps full
    detail for the requests that matter while cutting log volume and cost.
 2. **gRPC request/response logging.** Request and response payloads are logged as structured
    JSON, with per-method granularity, message counts, serialized sizes, and synthesized
    error trailers. (HTTP request/response logging is handled by Zalando Logbook.)
 3. **Cloud-correct structured JSON (GCP & Azure).** SLF4J levels map to the cloud `severity`
    field, with the extra error levels escalating to GCP `CRITICAL`/`ALERT` for paging; stack
    traces are folded inline into `message` so GCP Error Reporting groups them; trace/span
    fields are renamed to the cloud's conventions; and Fluentbit/Cloud Logging quirks (such as
    duplicate keys causing log loss and the 256 KB line limit) are worked around. See
    [`AGENTS.md`](AGENTS.md) for the full mapping.
 4. **Cross-thread correlation propagation.** gRPC request context (correlation-id, trace)
    lives in the gRPC `Context` rather than the thread-local MDC, because handlers hop threads
    (e.g. Netty event-loop threads). It is captured into the MDC at the moment the log event
    is snapshotted for asynchronous processing, so correlation fields survive the hop to the
    background writer thread without leaking MDC between threads.

Features:

 * SLF4J `Logger`-wrapper for additional error levels
 * Plug and play logging scheme
   * Main scope (for production):
     * Machine-readable JSON
   * Test scope (for local development). Log output can be toggled at runtime:
     * Human-readable 'classic' (one-line + ANSI colors, enabled by default), or
     * Human-readable JSON (pretty-printed + ANSI colored), or
     * Machine-readable JSON (single line)
 * Request-response-logging
     * Logbook style output (i.e. inlined in JSON log statements)
     * Additional ANSI coloring for test scope
 * Selective 'on-demand' logging for unexpected web server behaviour
     * capture full logs for problematic requests (i.e. not only WARN or ERROR, but also all sibling INFO log statements). Triggers:
        * HTTP status code
        * Log level (i.e. if ERROR logged, also log sibling INFO statements)
        * Call duration
        * Header present
        * Manual signalling
     * reduce cost of logging considerably
 * Unit testing
   * Always assert against machine-readable JSON 'under the hood', regardless what is printed to console during local development
   * Supported frameworks
     * JUnit 5
 * Correlation-id tracing

This library is mostly based on

 * SLF4J 2
 * Logback + Logback logstash
 * GRPC
   * Spring GRPC
 * [Logbook](https://github.com/zalando/logbook)

Supported web technologies are:

 * Spring web
 * Spring + gRPC

Supported clouds:

 * GCP (Stackdriver)
 * Azure

# License
[European Union Public Licence v1.2](https://eupl.eu/).

# Usage

## Overview
This project contains Spring boot starters which allows for property-based configuration:

 * base configuration (required)
 * request-response-logging (optional)
 * on-demand-logging (optional)

Each Spring Boot starter has a corresponding Spring Boot test starter. The Spring boot test starters enhance the developer experience during local development. 

The above starters are implemented in two flavours:

 * Servlet-based web
 * Netty-based gRPC

## Getting started
See [Getting started with gRPC](guides/gRPC.md) or [Getting started with servlet-based web](guides/web.md). Alternatively, go directly to the [examples](examples).

See also [troubleshooting](guides/troubleShooting.md).
# Roadmap

 * Replace the correlation-id tracing artifacts with standardized modern equivalents, i.e. span id and so on.
 * Better define and tune request-response logging JSON format
    * Add support for other text formats
    
# See also

 * [logback-logstash-syntax-highlighting-decorators](https://github.com/entur/logback-logstash-syntax-highlighting-decorators)



