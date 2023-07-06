package no.entur.logging.cloud.logback.logstash.test;

import java.io.Closeable;

/**
 *
 * Global log output console for use with testing.
 *
 */
public class CompositeConsoleOutputControl {

    private static final CompositeConsoleOutputControlClosable PLAIN = new CompositeConsoleOutputControlClosable();

    private static CompositeConsoleOutputType output = CompositeConsoleOutputType.humanReadablePlain;

    public static CompositeConsoleOutputType getOutput() {
        return output;
    }

    public static CompositeConsoleOutputControlClosable useHumanReadablePlainEncoder() {
        output = CompositeConsoleOutputType.humanReadablePlain;

        return PLAIN;
    }

    public static CompositeConsoleOutputControlClosable useHumanReadableJsonEncoder() {

        output = CompositeConsoleOutputType.humanReadableJson;

        return PLAIN;
    }

    public static CompositeConsoleOutputControlClosable useMachineReadableJsonEncoder() {
        output = CompositeConsoleOutputType.machineReadableJson;

        return PLAIN;
    }
}
