package no.entur.logging.cloud.logback.logstash.test;

/**
 *
 * Global log output console for use with testing.
 *
 */
public class CompositeConsoleOutputControl {

    private static CompositeConsoleOutputType output = CompositeConsoleOutputType.humanReadablePlain;

    public static CompositeConsoleOutputType getOutput() {
        return output;
    }

    public static void useHumanReadablePlainEncoder() {
        output = CompositeConsoleOutputType.humanReadablePlain;
    }

    public static void useHumanReadableJsonEncoder() {
        output = CompositeConsoleOutputType.humanReadableJson;
    }

    public static void useMachineReadableJsonEncoder() {
        output = CompositeConsoleOutputType.machineReadableJson;
    }
}
