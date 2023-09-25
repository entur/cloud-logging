package no.entur.logging.cloud.logbook.ondemand.state;

/**
 * Allow the request-response logger to trust the incoming JSON as well-formed in certain cases.
 *
 * For example, if the JSON payload is from an authenticated user, or if this service is purely internal,
 * or if a previous body filter always outputs valid JSON (without newlines), the request always contains wellformed JSON.
 */
@FunctionalInterface
public interface RequestHttpMessageStateSupplierSource {

    HttpMessageStateSupplier get();
}
