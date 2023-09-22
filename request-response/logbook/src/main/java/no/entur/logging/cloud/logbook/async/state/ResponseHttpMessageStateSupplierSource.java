package no.entur.logging.cloud.logbook.async.state;

/**
 * Allow the request-response logger to trust the outgoing JSON in certain cases.
 */

@FunctionalInterface
public interface ResponseHttpMessageStateSupplierSource {

    HttpMessageStateSupplier get();
}
