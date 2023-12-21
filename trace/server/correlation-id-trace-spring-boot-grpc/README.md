# Correlation-id tracing for Spring Boot / gRPC
`ServerInterceptor` features: 

 * Create or copy correlation-id from incoming requests to gRPC MDC.
   * Optionally fail the request if no correlation-id header 
 * Add correlation-id to response.

`ClientInterceptor` features:
 * add the correlation-id to request headers.
    * from the current gRPC MDC  
 
