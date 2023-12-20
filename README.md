# cloud-logging
Logging libraries for JVM applications. Goals

 * make developers more productive when developing and maintaining applications
 * keep cloud logging costs within reason
 * help comply with privacy constraints for logging

Features:

 * Cloud-specific log encoders
   * GCP (Stackdriver)
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
     * capture full logs for problematic requests
     * reduce logging considerably
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

# License
[European Union Public Licence v1.2](https://eupl.eu/).

# Feature details

## Logger-wrapper: Additional error levels
Most cloud logging backends support more error levels than SFL4J. 

This library lets developers add some more details about the seriousness of the errors:

* Tell me tomorrow (default error logging)
    * Handled by devops team during next available work hours
* Interrupt my dinner
    * Handled by devops team if within work hours, otherwise
    * Handled by operations team during wake hours
* Wake me up right now
    * Handled by devops team if within work hours, otherwise
    * Handled by operations team during wake or sleep hours

## On-demand logging
Enable "on-demand" logging for unexpected web server behaviour:

  * Caches log statements for each request in memory, then
  * throws them away for successful responses, or
  * logs them in case of
      * failed responses (i.e. HTTP status code >= 400) and/or
      * log events of a certain level (i.e. warning or error) was made
      * troubleshooting flag passed a header

Advantages:

  * reduced logging for happy cases
  * more "sibling" log statements for non-happy cases (i.e. not just WARN or ERROR log statements)
  * reduces the amount of work necessary to guarantee well-formed JSON log statements for request-response logging
      * skips JSON syntax check for throw-away request-response log statements, and/or
      * piggybacks on Spring REST framework databinding JSON syntax check 

# Cloud adaptations

## GCP
Stackdriver 

 * JSON encoder
 * Max log statement size (for request-response log statements)

See [GCP](gcp) for further details.

# Known issues
Hosting gRPC and web services (i.e. REST services other than actuator) in the same app is not supported.

# Roadmap

 * Replace the correlation-id tracing artifacts with standardized modern equivalents, i.e. span id and so on.
 * Better define and tune request-response logging JSON format 



