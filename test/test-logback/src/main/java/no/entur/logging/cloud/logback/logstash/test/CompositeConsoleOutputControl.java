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

    private static final CompositeConsoleValidationControlClosable VALIDATION_ENABLED = new CompositeConsoleValidationControlClosable(true);
    private static final CompositeConsoleValidationControlClosable VALIDATION_DISABLED = new CompositeConsoleValidationControlClosable(false);

    private static CompositeConsoleOutputType output = CompositeConsoleOutputType.humanReadablePlain;

    /**
     * Whether log statements are validated by the {@link LoggingEventValidator} of
     * {@link CompositeConsoleAsyncAppenderLogging}, if there is one. Read for every log statement, so
     * volatile: statements are validated on the thread which logs them, not the one which toggles this.
     */
    private static volatile boolean validationEnabled = true;

    /**
     * @return true if log statements are validated (the default), false otherwise
     */
    public static boolean isValidationEnabled() {
        return validationEnabled;
    }

    /**
     * Switch log statement validation on or off for the rest of the JVM, or until changed again.
     * <p>
     * This is deliberately a static switch rather than part of the logback configuration or a Spring
     * property: logging is global state, and logback is re-initialized (replacing the appenders) every time
     * Spring Boot creates a new application context, so anything configured per appender or per context
     * would apply to whichever context was created last. Prefer {@link #disableValidation()} (or its
     * variants), which restores the previous state.
     *
     * @param validationEnabled true to validate log statements, false to skip validation
     */
    public static void setValidationEnabled(boolean validationEnabled) {
        CompositeConsoleOutputControl.validationEnabled = validationEnabled;
    }

    /**
     * Skip log statement validation until the returned object is closed, which restores the previous state.
     * Intended for code (or third-party libraries) known to log statements which fail validation.
     * <pre>
     * try (Closeable c = CompositeConsoleOutputControl.disableValidation()) {
     *     // test code
     * }
     * </pre>
     * Applies to all threads, so tests which run in parallel in the same JVM affect each other.
     *
     * @return a closable which restores the previous state
     */
    public static CompositeConsoleValidationControlClosable disableValidation() {
        boolean previous = isValidationEnabled();
        setValidationEnabled(false);
        return previous ? VALIDATION_ENABLED : VALIDATION_DISABLED;
    }

    public static void disableValidation(Consumer r) throws Exception {
        try (CompositeConsoleValidationControlClosable c = disableValidation()) {
            r.run();
        }
    }

    public static <V> V disableValidation(Callable<V> r) throws Exception {
        try (CompositeConsoleValidationControlClosable c = disableValidation()) {
            return r.call();
        }
    }

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
