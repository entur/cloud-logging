# request-response-logging for Spring Web
Relies on Logbook. 

Logbook does not handle ERROR dispatch, so all errors must be handled, or response logging will not be performed.

While we would like to avoid error-handling in this library, adding `@ControllerAdvice` seems like the most practical solution.

So this configuration includes a `@ControllerAdvice` for `Throwable`, which is enabled by default. 
This `@ControllerAdvice` also handles a set of more specific exceptions, including `AccessDeniedException`, `AuthenticationException` and the default set from Spring's `ResponseEntityExceptionHandler`. 

Opt-out by setting `entur.logging.request-response.http.server.controller-advice.enabled` to false.