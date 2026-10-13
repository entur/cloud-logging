package no.entur.logging.cloud.azure.spring.test;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import net.logstash.logback.marker.Markers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the {@code logback-test.xml} shipped by this module, i.e. that the field name validation is
 * wired onto the console appender, and can be switched off.
 */
class LogbackTestConfigurationTest {

    private LoggerContext context;

    @AfterEach
    void stopContext() {
        if (context != null) {
            context.stop();
        }
    }

    private Logger configure(String enabledPropertyValue) throws Exception {
        context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        if (enabledPropertyValue != null) {
            // equivalent to -D<property>=<value>, which logback's variable substitution also resolves
            context.putProperty(AzureLogStatementFieldNameValidator.ENABLED_PROPERTY, enabledPropertyValue);
        }
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(getClass().getResource("/logback-test.xml"));

        return context.getLogger(LogbackTestConfigurationTest.class);
    }

    @Test
    void validatesLogStatementsByDefault() throws Exception {
        Logger logger = configure(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> logger.info(Markers.append("message", "oops"), "an order"));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validatesLogStatementsWhenExplicitlyEnabled() throws Exception {
        Logger logger = configure("true");

        assertThrows(IllegalStateException.class,
                () -> logger.info(Markers.append("message", "oops"), "an order"));
    }

    @Test
    void validDoesNotThrow() throws Exception {
        Logger logger = configure(null);

        assertDoesNotThrow(() -> logger.info(Markers.append("orderId", "123"), "an order"));
    }

    @Test
    void validationCanBeDisabled() throws Exception {
        Logger logger = configure("false");

        assertDoesNotThrow(() -> logger.info(Markers.append("message", "oops"), "an order"));
    }

    @Test
    void validationCanBeDisabledWithSystemProperty() throws Exception {
        System.setProperty(AzureLogStatementFieldNameValidator.ENABLED_PROPERTY, "false");
        try {
            Logger logger = configure(null);

            assertDoesNotThrow(() -> logger.info(Markers.append("message", "oops"), "an order"));
        } finally {
            System.clearProperty(AzureLogStatementFieldNameValidator.ENABLED_PROPERTY);
        }
    }
}
