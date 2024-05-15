![Build Status](https://github.com/entur/cloud-logging/actions/workflows/gradle.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/no.entur.logging.cloud/api.svg)](https://mvnrepository.com/artifact/no.entur.logging.cloud)

# cloud-logging
Logging libraries for JVM applications. Goals

 * make developers more productive when developing and maintaining applications
 * keep cloud logging costs within reason
 * help comply with privacy constraints for logging

Features:

 * SLF4J `Logger`-wrapper for additional error levels
 * Plug and play logging scheme
   * Main scope (for production):
     * Machine-readable JSON
   * Test scope (for local development). Log output can be toggeled at runtime:
     * Human-readable 'classic' (one-line + ANSI colors, enabled by default), or
     * Human-readable JSON (pretty-printed + ANSI colored), or
     * Machine-readable JSON (single line)
 * Request-response-logging
     * Logbook style output (i.e. inlined in JSON log statements)
     * Additional ANSI coloring for test scope
 * Selective 'on-demand' logging for unexpected web server behaviour
     * capture full logs for problematic requests (i.e. not only WARN or ERROR, but also all sibling INFO log statements)
     * reduce cost of logging considerably
 * Unit testing
   * Always assert against machine-readable JSON 'under the hood', regardless what is printed to console during local development
   * Supported frameworks
     * JUnit 5
 * Correlation-id tracing

This library is mostly based on

 * SLF4J 2
 * Logback + Logback logstash
 * Lognet GRPC
 * Logbook

Supported web technologies are:

 * Spring web
 * Spring + gRPC via Lognet
 * Micrometer

Supported clouds:

 * GCP (stackdriver)

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
 * Lognet gRPC

## Getting started
See [Getting started with gRPC](guides/gRPC.md) or [Getting started with servlet-based web](guides/web.md)

# Roadmap

 * Replace the correlation-id tracing artifacts with standardized modern equivalents, i.e. span id and so on.
 * Better define and tune request-response logging JSON format 



