# GCP trace headers with legacy correlation-id tracing
Adds `logging.googleapis.com/trace` and `logging.googleapis.com/spanId` to MDC context.

Automatically disabled if opentelementry is on the classpath or loaded as an agent.

