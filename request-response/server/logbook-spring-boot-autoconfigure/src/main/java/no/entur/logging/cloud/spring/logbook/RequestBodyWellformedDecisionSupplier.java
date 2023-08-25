package no.entur.logging.cloud.spring.logbook;

import java.util.function.BooleanSupplier;

/**
 * Allow the request-response logger to trust the incoming JSON as well-formed in certain cases.
 *
 * For example, if the JSON payload is from an authenticated user, or if this service is purely internal,
 * or if a previous body filter always outputs valid JSON (without newlines), the request always contains wellformed JSON.
 */
public interface RequestBodyWellformedDecisionSupplier extends BooleanSupplier {
}
