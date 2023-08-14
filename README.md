# cloud-logging
Entur cloud logging libraries. Based on

 * SLF4J 2
 * Logback
 * Logback logstash

with

 * Cloud-specific log encoders (output to console)
   * GCP (Stackdriver)
   * Azure (TODO)
 * `DevOpsLogger` extension for additional error levels 
   * for interaction with devops / ops.
 * Friendly logging scheme
   * Main scope (for deployments):
     * Machine-readable JSON 
   * Test scope (for local development). Log output can be toggeled at runtime:
     * Human-readable 'classic' one-line (default)
     * Human-readable JSON (i.e. pretty-printed + colored JSON)
     * Machine-readable JSON
 * Request-response-logging
   * Logbook style output
   * Additional test scope coloring
 *  On-demand logging for failed request
   * Caches logged events, then
   * throws them away for success responses, or
   * logs them for failure responses (i.e. HTTP status code >= 400)
 * Unit testing (assert against Machine-readable JSON 'under the hoood')
   * JUnit 5
   * Assertj (TODO)
   * Truth (TODO)
 * Correlation-id tracing

Supported web technologies:

 * Spring web
 * Spring gRPC via Lognet
   * With custom MDC support 
 * Micrometer
 * Spring webflux (TODO)

## Additional error levels
Most cloud logging backends supports more error levels than SFL4J. 

This library lets developers add some more details about the seriousness of the errors:

* Tell me tomorrow (default error logging)
    * Handled by devops team during next available work hours
* Interrupt my dinner
    * Handled by devops team if within work hours, otherwise
    * Handled by operations team, during wake hours
* Wake me up right now
    * Handled by devops team if within work hours, otherwise
    * Handled by operations team, during wake or sleep hours

Obviously the operations teams can get hold of the devops team when necessary.

# Cloud adaptations

## GCP
Stackdriver 

 * JSON encoder
 * Max log statement size

## Azure
TODO

