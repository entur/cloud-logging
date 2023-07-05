# cloud-logging
Entur cloud logging libraries.

## SLF4J
`Logger` extension for additional error levels.

# Cloud adaptations

## GCP
Stackdriver 

 * JSON encoder
 * max log statement size

## Azure
TODO


# TODO
Request-response logging mode:

  - always
  - on-error (when response contains an error code, log both the request and the response)
  - never

Configurable micrometer metric prefix
 - gcp
 - devops