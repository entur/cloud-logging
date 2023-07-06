package no.entur.logging.cloud.logback.logstash.test;

import org.slf4j.Marker;

/**
 *
 * Type to improve logbook test output.
 *
 * Captures the desired output mode as the log statement is created.
 *
 * Log statements without this marker are printed using the default type.
 *
 */

public interface CompositeConsoleOutputMarker extends Marker {

    CompositeConsoleOutputType getCompositeConsoleOutputType();
}
