package no.entur.logging.cloud.logback.logstash.test;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 * Global log output console for use with testing.
 *
 */
public class CompositeConsoleOutputControlClosable implements Closeable {

    @Override
    public void close() {
        CompositeConsoleOutputControl.useHumanReadablePlainEncoder();
    }
}
