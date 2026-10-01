package no.entur.logging.cloud.gcp.spring.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import no.entur.logging.cloud.appender.MdcContributer;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAsyncAppenderLogging;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputType;
import no.entur.logging.cloud.logback.logstash.test.DefaultCompositeConsoleOutputLoggingEvent;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link LogStatementFieldNameValidator} against the actual logstash-logback {@code Marker}/
 * {@code StructuredArgument} types that applications use to attach fields to a log statement, as well as
 * MDC, covering both the "forbidden" (reserved by the encoder) and "duplicate" cases.
 */
class LogStatementFieldNameValidatorTest {

    private final LoggerContext context = new LoggerContext();
    private final Logger logger = context.getLogger(LogStatementFieldNameValidatorTest.class);

    private LoggingEvent newEvent(String message, Marker marker, Object... arguments) {
        return newEvent(message, marker, Map.of(), arguments);
    }

    private LoggingEvent newEvent(String message, Marker marker, Map<String, String> mdc, Object... arguments) {
        LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO, message, null, arguments);
        if (marker != null) {
            event.addMarker(marker);
        }
        event.setMDCPropertyMap(mdc);
        return event;
    }

    private LoggingEvent newEventWithMdc(Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO, "a message", null, null);
        event.setMDCPropertyMap(mdc);
        return event;
    }

    private LoggingEvent newRealEventForMdcSnapshot() {
        Logger logger = (Logger) LoggerFactory.getLogger(LogStatementFieldNameValidatorTest.class);
        return new LoggingEvent("fqcn", logger, Level.INFO, "msg", null, null);
    }

    // --- MDC ---

    @Test
    void validate_mdcKeyCollidingWithReservedMessageField_throwsForbiddenField() {
        LoggingEvent event = newEventWithMdc(Map.of("message", "oops"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_uniqueMdcField_doesNotThrow() {
        LoggingEvent event = newEventWithMdc(Map.of("myAppField", "value"));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_runsAfterContributedMdcHasBeenSnapshotted() {
        CompositeConsoleAsyncAppenderLogging appender = new CompositeConsoleAsyncAppenderLogging();
        appender.setMdcContributer(new MdcContributer() {
            @Override
            public Map<String, String> getMdc() {
                return Map.of("grpcKey", "grpcValue");
            }
        });
        appender.setValidator(event -> assertThat(event.getMDCPropertyMap()).containsEntry("grpcKey", "grpcValue"));

        LoggingEvent event = newRealEventForMdcSnapshot();

        assertDoesNotThrow(() -> appender.preprocess(new DefaultCompositeConsoleOutputLoggingEvent(event, CompositeConsoleOutputType.humanReadableJson)));
    }

    // --- StructuredArguments.kv / Markers.append (SingleFieldAppendingMarker fast path) ---

    @Test
    void validate_structuredArgumentKvUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.kv("severity", "CRITICAL"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
        assertThat(exception).hasMessageThat().contains("severity");
    }

    @Test
    void validate_twoStructuredArgumentsWithSameFieldName_throwsDuplicateField() {
        LoggingEvent event = newEvent("msg {} {}", null,
                StructuredArguments.kv("orderId", "123"),
                StructuredArguments.kv("orderId", "456"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
        assertThat(exception).hasMessageThat().contains("orderId");
    }

    @Test
    void validate_markerAppendWithUniqueFieldName_doesNotThrow() {
        Marker marker = Markers.append("orderId", "123");
        LoggingEvent event = newEvent("msg", marker);

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_markerAppendCollidingWithStructuredArgument_throwsDuplicateField() {
        Marker marker = Markers.append("orderId", "from-marker");
        LoggingEvent event = newEvent("msg {}", marker, StructuredArguments.kv("orderId", "from-argument"));

        assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
    }

    // --- StructuredArguments.entries / Markers.appendEntries (MapEntriesAppendingMarker) ---

    @Test
    void validate_entriesWithUniqueKeys_doesNotThrow() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("field1", "value1");
        map.put("field2", "value2");
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.entries(map));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_entriesUsingReservedFieldName_throwsForbiddenField() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", "duplicated");
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.entries(map));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_nestedMapValueReusingRootFieldName_doesNotThrow() {
        // fields nested inside a map value may re-use a name already used at the root - this is exactly
        // the documented workaround for avoiding root-level collisions.
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("message", "not a duplicate, nested");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("myApp", nested);

        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.entries(map));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_appendEntriesMarkerCollidingWithMdc_throwsDuplicateField() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("correlationId", "from-marker");

        LoggingEvent event = newEvent("msg", Markers.appendEntries(map), Map.of("correlationId", "from-mdc"));

        assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_mdcKeyLiterallyUsingGcpTraceField_doesNotThrow() {
        // GCP trace field names are a supported, legitimate raw MDC key - used directly by e.g.
        // GcpTraceFilter/GcpTraceMdcSupport and the gRPC trace interceptors so the default MDC provider
        // writes the field straight through - so this must not be flagged.
        LoggingEvent event = newEvent("msg", null, Map.of("logging.googleapis.com/trace", "abc123"));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_markerUsingGcpTraceFieldName_throwsForbiddenField() {
        // Unlike MDC, there is no legitimate way to contribute a GCP tracing field via a Marker/structured
        // argument, so it remains forbidden there.
        LoggingEvent event = newEvent("msg {}", null, Map.of(), StructuredArguments.kv("logging.googleapis.com/trace", "abc123"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_mdcGcpTraceFieldCollidingWithMarker_throwsField() {
        // The MDC value is legitimate on its own, but a marker also contributing the very same field name
        // must still be rejected (as the marker itself uses a forbidden field name here).
        LoggingEvent event = newEvent("msg {}", null, Map.of("logging.googleapis.com/trace", "abc123"),
                StructuredArguments.kv("logging.googleapis.com/trace", "def456"));

        assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
    }

    // --- application-defined LogstashMarker subclasses (not StructuredArgument) ---

    /** A minimal custom marker, as an application might define, that is a {@link LogstashMarker} but not a
     * {@code StructuredArgument}. */
    private static final class CustomFieldMarker extends LogstashMarker {

        private final String fieldName;
        private final String value;

        CustomFieldMarker(String fieldName, String value) {
            super("CUSTOM_FIELD");
            this.fieldName = fieldName;
            this.value = value;
        }

        @Override
        public void writeTo(JsonGenerator generator) throws JacksonException {
            generator.writeStringProperty(fieldName, value);
        }
    }

    @Test
    void validate_customLogstashMarkerSubclassUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg", new CustomFieldMarker("message", "oops"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_customLogstashMarkerSubclassCollidingWithStructuredArgument_throwsDuplicateField() {
        Marker marker = new CustomFieldMarker("orderId", "from-custom-marker");
        LoggingEvent event = newEvent("msg {}", marker, StructuredArguments.kv("orderId", "from-argument"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
    }

    @Test
    void validate_customLogstashMarkerSubclassWithUniqueFieldName_doesNotThrow() {
        LoggingEvent event = newEvent("msg", new CustomFieldMarker("orderId", "123"));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }
}
