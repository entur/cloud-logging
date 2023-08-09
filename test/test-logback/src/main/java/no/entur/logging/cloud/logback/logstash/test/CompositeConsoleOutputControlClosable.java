package no.entur.logging.cloud.logback.logstash.test;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 * Global log output console for use with testing.
 *
 */
public class CompositeConsoleOutputControlClosable implements Closeable {


    private final CompositeConsoleOutputType output;

    public CompositeConsoleOutputControlClosable(CompositeConsoleOutputType output) {
        this.output = output;
    }

    @Override
    public void close() {
        CompositeConsoleOutputControl.setOutput(output);
    }
}
