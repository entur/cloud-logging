package no.entur.logging.cloud.gcp.spring.test;

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
 * validator, or a log statement re-using that name would go undetected. Fails if a provider is added to (or
 * a field renamed in) the encoder configuration without updating the validator.
 *
 * <p>Only a superset is asserted: the validator is deliberately conservative, e.g. {@code @version} and
 * {@code @timestamp} are disabled in the shipped configuration, and {@code serviceContext} is only written
 * when a service has been configured.
 */
class ReservedFieldNamesTest {

    private LoggerContext context;

    @AfterEach
    void stopContext() {
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
        // everything which can make a built-in provider write something: an exception (message with
        // stacktrace) and tracing MDC (remapped to the GCP trace fields), but no application fields
        LoggingEvent event = new LoggingEvent(Logger.class.getName(), logger, Level.ERROR, "an error", new RuntimeException("boom"), null);
        event.setMDCPropertyMap(Map.of("traceId", "t", "spanId", "s", "traceSampled", "true"));

        Set<String> names = new TreeSet<>();
        JsonMapper.builder().build().readTree(encoder.encode(event)).propertyNames().forEach(names::add);
        return names;
    }

    private static void assertAccountedFor(Set<String> writtenFieldNames) {
        Set<String> accountedFor = new TreeSet<>(LogStatementFieldNameValidator.RESERVED_FIELD_NAMES);
        accountedFor.addAll(LogStatementFieldNameValidator.GCP_TRACE_FIELD_NAMES);

        Set<String> unaccountedFor = new TreeSet<>(writtenFieldNames);
        unaccountedFor.removeAll(accountedFor);

        // sanity check: guards against the test silently passing for want of output
        assertThat(writtenFieldNames).containsAtLeast("severity", "message", "timestamp");
        assertThat(unaccountedFor).isEmpty();
    }

    @Test
    void reservedFieldNamesCoverEveryRootFieldWrittenByTheProductionEncoder() throws Exception {
        String xml = "<configuration><include resource=\"logback/logback-default-server.xml\"/>"
                + "<root><appender-ref ref=\"STDOUT\"/></root></configuration>";
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(newContext());
        configurator.doConfigure(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        @SuppressWarnings("unchecked")
        ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("STDOUT");

        assertAccountedFor(rootFieldNamesWrittenBy(appender.getEncoder(), context.getLogger("some.Logger")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reservedFieldNamesCoverEveryRootFieldWrittenByTheTestEncoder() throws Exception {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(newContext());
        configurator.doConfigure(getClass().getResource("/logback-test.xml"));

        CompositeConsoleAsyncAppenderLogging async = (CompositeConsoleAsyncAppenderLogging) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("STDOUT_ASYNC");
        SpringCompositeConsoleAppender console = (SpringCompositeConsoleAppender) async.getAppender("STDOUT");

        // the machine readable encoder is the one assertions are evaluated against, and mirrors production
        assertAccountedFor(rootFieldNamesWrittenBy(console.getMachineReadableJsonEncoder(), context.getLogger("some.Logger")));
    }
}
