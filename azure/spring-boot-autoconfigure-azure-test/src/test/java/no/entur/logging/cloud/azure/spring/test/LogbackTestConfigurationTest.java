package no.entur.logging.cloud.azure.spring.test;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import net.logstash.logback.marker.Markers;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Closeable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the {@code logback-test.xml} shipped by this module, i.e. that the field name validation is
 * wired onto the console appender, and can be switched off with {@link CompositeConsoleOutputControl}.
 */
class LogbackTestConfigurationTest {

    private LoggerContext context;

    @AfterEach
    void cleanUp() {
        CompositeConsoleOutputControl.setValidationEnabled(true);
        if (context != null) {
            context.stop();
        }
    }

    private Logger configure() throws Exception {
        context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(getClass().getResource("/logback-test.xml"));

        return context.getLogger(LogbackTestConfigurationTest.class);
    }

    private static void logOffendingStatement(Logger logger) {
        logger.info(Markers.append("message", "oops"), "an order");
    }

    private static void logValidStatement(Logger logger) {
        logger.info(Markers.append("orderId", "123"), "an order");
    }

    @Test
    void validatesLogStatementsByDefault() throws Exception {
        Logger logger = configure();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> logOffendingStatement(logger));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validDoesNotThrow() throws Exception {
        Logger logger = configure();

        assertDoesNotThrow(() -> logValidStatement(logger));
    }

    @Test
    void validationCanBeSwitchedOffAtRuntime() throws Exception {
        Logger logger = configure();

        try (Closeable c = CompositeConsoleOutputControl.disableValidation()) {
            assertDoesNotThrow(() -> logOffendingStatement(logger));
        }

        // closing restores the previous state
        assertThrows(IllegalStateException.class, () -> logOffendingStatement(logger));
    }

    @Test
    void validationStaysSwitchedOffWhenLoggingIsReinitialized() throws Exception {
        try (Closeable c = CompositeConsoleOutputControl.disableValidation()) {
            configure();

            // Spring Boot does this for every new application context: a new configuration, with new appenders
            context.stop();
            Logger logger = configure();

            assertDoesNotThrow(() -> logOffendingStatement(logger));
        }
    }
}
