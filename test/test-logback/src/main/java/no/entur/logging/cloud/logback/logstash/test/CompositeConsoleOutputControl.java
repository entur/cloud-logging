package no.entur.logging.cloud.logback.logstash.test;

import java.io.Closeable;
import java.util.concurrent.Callable;

/**
 *
 * Global log output console for use with testing.
 *
 */
public class CompositeConsoleOutputControl {

    @FunctionalInterface
    public interface Consumer {
        void run() throws Exception;
    }

    private static final CompositeConsoleOutputControlClosable HUMAN_READABLE_PLAIN = new CompositeConsoleOutputControlClosable(CompositeConsoleOutputType.humanReadablePlain);
    private static final CompositeConsoleOutputControlClosable HUMAN_READABLE_JSON = new CompositeConsoleOutputControlClosable(CompositeConsoleOutputType.humanReadableJson);
    private static final CompositeConsoleOutputControlClosable MACHINE_READABLE_JSON = new CompositeConsoleOutputControlClosable(CompositeConsoleOutputType.machineReadableJson);

    private static CompositeConsoleOutputType output = CompositeConsoleOutputType.humanReadablePlain;

    public static CompositeConsoleOutputType getOutput() {
        return output;
    }

    public static void setOutput(CompositeConsoleOutputType output) {
        CompositeConsoleOutputControl.output = output;
    }

    public static CompositeConsoleOutputControlClosable useHumanReadablePlainEncoder() {
        CompositeConsoleOutputType output = getOutput();
        try {
            setOutput(CompositeConsoleOutputType.humanReadablePlain);
        } finally {
            return toClosable(output);
        }
    }

    public static void useHumanReadablePlainEncoder(Consumer r) throws Exception {
        try (CompositeConsoleOutputControlClosable c = useHumanReadablePlainEncoder()) {
            r.run();
        }
    }

    public static <V> V useHumanReadablePlainEncoder(Callable<V> r) throws Exception {
        try (CompositeConsoleOutputControlClosable c = useHumanReadablePlainEncoder()) {
            return r.call();
        }
    }

    public static CompositeConsoleOutputControlClosable useHumanReadableJsonEncoder() {
        CompositeConsoleOutputType output = getOutput();
        try {
            setOutput(CompositeConsoleOutputType.humanReadableJson);
        } finally {
            return toClosable(output);
        }
    }

    public static void useHumanReadableJsonEncoder(Consumer r) throws Exception {
        try (CompositeConsoleOutputControlClosable c = useHumanReadableJsonEncoder()) {
            r.run();
        }
    }

    public static <V> V useHumanReadableJsonEncoder(Callable<V> r) throws Exception {
        try (CompositeConsoleOutputControlClosable c = useHumanReadableJsonEncoder()) {
            return r.call();
        }
    }

    public static CompositeConsoleOutputControlClosable useMachineReadableJsonEncoder() {
        CompositeConsoleOutputType output = getOutput();
        try {
            setOutput(CompositeConsoleOutputType.machineReadableJson);
        } finally {
            return toClosable(output);
        }
    }

    public static void useMachineReadableJsonEncoder(Consumer r) throws Exception {
        try (CompositeConsoleOutputControlClosable c = useMachineReadableJsonEncoder()) {
            r.run();
        }
    }

    public static <V> V useMachineReadableJsonEncoder(Callable<V> r) throws Exception {
        try (CompositeConsoleOutputControlClosable c = useMachineReadableJsonEncoder()){
            return r.call();
        }
    }

    private static CompositeConsoleOutputControlClosable toClosable(CompositeConsoleOutputType output) {
        switch (output) {
            case humanReadableJson: return HUMAN_READABLE_JSON;
            case machineReadableJson: return MACHINE_READABLE_JSON;
            case humanReadablePlain: return HUMAN_READABLE_PLAIN;

            default: throw new IllegalStateException("Unexpected output type " + output);
        }
    }
}
