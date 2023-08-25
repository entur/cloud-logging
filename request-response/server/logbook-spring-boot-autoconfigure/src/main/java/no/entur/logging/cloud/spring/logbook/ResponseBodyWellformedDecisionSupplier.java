package no.entur.logging.cloud.spring.logbook;

import java.util.function.BooleanSupplier;

/**
 * Allow the request-response logger to trust the outgoing JSON in certain cases.
 */
public interface ResponseBodyWellformedDecisionSupplier extends BooleanSupplier {
}
