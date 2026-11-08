package no.entur.logging.cloud.logback.logstash.test;

import java.io.Closeable;

/**
 *
 * Restores the previous log statement validation state, see {@link CompositeConsoleOutputControl#disableValidation()}.
 *
 */
public class CompositeConsoleValidationControlClosable implements Closeable {

    private final boolean validationEnabled;

    public CompositeConsoleValidationControlClosable(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    @Override
    public void close() {
        CompositeConsoleOutputControl.setValidationEnabled(validationEnabled);
    }
}
