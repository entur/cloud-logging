package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.core.encoder.Encoder;

/**
 *
 * Global log output console for use with testing.
 *
 */
public class CompositeConsoleOutputControl {

    private static CompositeConsoleOutputType output = CompositeConsoleOutputType.humanReadablePlainEncoder;

    public static CompositeConsoleOutputType getOutput() {
        return output;
    }

    public static void useHumanReadablePlainEncoder() {
        output = CompositeConsoleOutputType.humanReadablePlainEncoder;
    }

    public static void useHumanReadableJsonEncoder() {
        output = CompositeConsoleOutputType.humanReadableJsonEncoder;
    }

    public static void useMachineReadableJsonEncoder() {
        output = CompositeConsoleOutputType.machineReadableJsonEncoder;
    }
}
