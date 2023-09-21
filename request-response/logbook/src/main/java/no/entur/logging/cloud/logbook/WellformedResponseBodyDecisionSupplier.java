package no.entur.logging.cloud.logbook;

import java.util.function.BooleanSupplier;

/**
 * Allow the request-response logger to trust the outgoing JSON in certain cases.
 */

@FunctionalInterface
public interface WellformedResponseBodyDecisionSupplier {

    BooleanSupplier get();
}
