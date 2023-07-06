package no.entur.logging.cloud.logback.logstash.test;

import org.slf4j.Marker;

/**
 *
 * Type to improve logbook test output.
 *
 * Captures the desired output mode as the log statement is created.
 * The logbook log messages are adapted to the encoder (plain / JSON), so should be printed
 * using the right encoder.
 *
 * Log statements without this marker are printed using the default type.
 *
 */

public interface CompositeConsoleOutputMarker extends Marker {

    CompositeConsoleOutputType getCompositeConsoleOutputType();
}
