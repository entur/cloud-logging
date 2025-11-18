# request-response-logging for Spring Web
Relies on Logbook. 

Logbook does not handle ERROR dispatch, so all errors must be handled. 

This configuration includes a `@ControllerAdvice` for `Throwable`, which is enabled by default.

Opt-out by setting `entur.logging.request-response.http.server.controller-advice.enabled` to false.