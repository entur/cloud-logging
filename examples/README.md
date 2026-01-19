# examples
Examples of Spring Boot applications hosting some kind of service:

 * gRPC 
 * REST

or both. There is also examples of application without test artifacts, as examples of what a deployed application will actually see.

Please note:

 * The examples have unit tests which try out various configuration settings
 * For local development, a shortened 'classic' one-line is the default
 * To examine log statements in more details use `CompositeConsoleOutputControl.useHumanReadableJsonEncoder()` for JSON output

# Spring gRPC interceptors
The gRPC interceptor order overview:

## Service configuration

```
@GrpcService(interceptors = {

  // Add trace headers (correlation-id and such)
  OrderedCorrelationIdGrpcMdcContextServerInterceptor.class,

  // Logging
  OrderedGrpcLoggingServerInterceptor.class,
  RequestResponseGrpcExceptionHandlerInterceptor.class,

  // Validation
  MyValidationServerInterceptor.class,

})
```

## Interceptor order
Note that interceptors are sorted using the `Ordered` interface. Recommended setup; __global in bold__ (cannot be disabled)

* __0__: GrpcExceptionInterceptor - GRPC_GLOBAL_EXCEPTION_HANDLING
    * Exception advice etc
* 50: `OrderedCorrelationIdGrpcMdcContextServerInterceptor`
    * cloud-logging
    * `entur.logging.grpc.trace.mdc.interceptor-order`
* __2500__: GRPC_TRACING_METRICS (global)
    * included in spring-grpc
* __5000__: GRPC_SECURITY_EXCEPTION_HANDLING
    * included in spring-grpc
* __5100__: GRPC_SECURITY_AUTHENTICATION
    * included in spring-grpc
* 5170: `OrderedGrpcLoggingServerInterceptor`
    * cloud-logging: Request-response logging
    * `entur.logging.request-response.grpc.server.interceptor-order`
* 5175: `RequestResponseGrpcExceptionHandlerInterceptor`
    * cloud-logging: request-response logging helper, wraps `GrpcExceptionInterceptor`
        * make sure `GRPC_GLOBAL_EXCEPTION_HANDLING` runs before logging response
    * `entur.logging.request-response.grpc.server.exception-handler.interceptor-order`
* __5200__: GRPC_SECURITY_AUTHORISATION
    * included in spring-grpc
* 6000: `MyValidationServerInterceptor`
    * grpc-validation: Validation of proto messages 

Corresponding properties

```
entur.logging.grpc.trace.mdc.interceptor-order=50
entur.logging.request-response.grpc.server.interceptor-order=5170
entur.logging.request-response.grpc.server.exception-handler.interceptor-order=5175
```

