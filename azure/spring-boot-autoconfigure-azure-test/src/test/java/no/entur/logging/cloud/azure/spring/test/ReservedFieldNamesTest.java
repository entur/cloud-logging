package no.entur.logging.cloud.azure.spring.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAsyncAppenderLogging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.truth.Truth.assertThat;

/**
 * Guards {@link LogStatementFieldNameValidator#RESERVED_FIELD_NAMES} against drifting away from what the
 * encoder actually writes: every field the encoder puts at the root of a log statement must be known to the
 * validator, and vice versa. Fails if a provider is added to (or removed from, or a field renamed in) the
 * encoder configuration without updating the validator.
 */
class ReservedFieldNamesTest {

    private static final String HOSTNAME_PROPERTY = "HOSTNAME";

    private LoggerContext context;
    private String originalHostname;

    @BeforeEach
    void setHostname() {
        // the service context is only written when a service could be detected from the hostname; the
        // encoder does so when it is configured, so this must be in place before the configuration is read
        originalHostname = System.getProperty(HOSTNAME_PROPERTY);
        System.setProperty(HOSTNAME_PROPERTY, "myservice-abc12-xyz");
    }

    @AfterEach
    void restore() {
        if (originalHostname != null) {
            System.setProperty(HOSTNAME_PROPERTY, originalHostname);
        } else {
            System.clearProperty(HOSTNAME_PROPERTY);
        }
        if (context != null) {
            context.stop();
        }
    }

    private LoggerContext newContext() {
        context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        return context;
    }

    private static Set<String> rootFieldNamesWrittenBy(Encoder<ILoggingEvent> encoder, Logger logger) throws Exception {
        // everything which can make a built-in provider write something: an exception (stackTrace and
        // exceptionClass) and MDC (nested in the mdc object), but no application fields
        LoggingEvent event = new LoggingEvent(Logger.class.getName(), logger, Level.ERROR, "an error", new RuntimeException("boom"), null);
        event.setMDCPropertyMap(Map.of("mdcField", "d"));

        Set<String> names = new TreeSet<>();
        JsonMapper.builder().build().readTree(encoder.encode(event)).propertyNames().forEach(names::add);
        return names;
    }

    @Test
    void reservedFieldNamesAreExactlyTheRootFieldsWrittenByTheProductionEncoder() throws Exception {
        String xml = "<configuration><include resource=\"logback/logback-default-server.xml\"/>"
                + "<root><appender-ref ref=\"STDOUT\"/></root></configuration>";
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(newContext());
        configurator.doConfigure(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        @SuppressWarnings("unchecked")
        ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("STDOUT");

        assertThat(rootFieldNamesWrittenBy(appender.getEncoder(), context.getLogger("some.Logger")))
                .containsExactlyElementsIn(LogStatementFieldNameValidator.RESERVED_FIELD_NAMES);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reservedFieldNamesAreExactlyTheRootFieldsWrittenByTheTestEncoder() throws Exception {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(newContext());
        configurator.doConfigure(getClass().getResource("/logback-test.xml"));

        CompositeConsoleAsyncAppenderLogging async = (CompositeConsoleAsyncAppenderLogging) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("STDOUT_ASYNC");
        SpringCompositeConsoleAppender console = (SpringCompositeConsoleAppender) async.getAppender("STDOUT");

        // the machine readable encoder is the one assertions are evaluated against, and mirrors production
        assertThat(rootFieldNamesWrittenBy(console.getMachineReadableJsonEncoder(), context.getLogger("some.Logger")))
                .containsExactlyElementsIn(LogStatementFieldNameValidator.RESERVED_FIELD_NAMES);
    }
}
