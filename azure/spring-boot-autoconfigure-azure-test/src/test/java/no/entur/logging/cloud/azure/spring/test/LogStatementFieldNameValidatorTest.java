package no.entur.logging.cloud.azure.spring.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.io.SerializedString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link LogStatementFieldNameValidator} against the logstash-logback {@code Marker} types that
 * applications use to attach root-level fields to a log statement, covering both the "forbidden" (reserved by
 * the encoder) and "duplicate" cases - and that what the Azure encoder does not write as root-level fields
 * (MDC, structured arguments, key/value pairs) is not flagged.
 */
class LogStatementFieldNameValidatorTest {

    private final LoggerContext context = new LoggerContext();
    private final Logger logger = context.getLogger(LogStatementFieldNameValidatorTest.class);

    private LoggingEvent newEvent(String message, Marker marker, Object... arguments) {
        LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO, message, null, arguments);
        if (marker != null) {
            event.addMarker(marker);
        }
        event.setMDCPropertyMap(Map.of());
        return event;
    }

    // --- Markers.append (SingleFieldAppendingMarker fast path) ---

    @ParameterizedTest
    @ValueSource(strings = {"timestamp", "logger", "level", "thread", "mdc", "message", "stackTrace", "exceptionClass", "serviceContext"})
    void validate_markerAppendUsingReservedFieldName_throwsForbiddenField(String fieldName) {
        LoggingEvent event = newEvent("msg", Markers.append(fieldName, "oops"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
        assertThat(exception).hasMessageThat().contains(fieldName);
    }

    @Test
    void validate_markerAppendWithUniqueFieldName_doesNotThrow() {
        LoggingEvent event = newEvent("msg", Markers.append("orderId", "123"));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_twoMarkersWithSameFieldName_throwsDuplicateField() {
        LoggingEvent event = newEvent("msg", Markers.append("orderId", "1"));
        event.addMarker(Markers.append("orderId", "2"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
        assertThat(exception).hasMessageThat().contains("orderId");
    }

    @Test
    void validate_chainedMarkersWithSameFieldName_throwsDuplicateField() {
        // references, i.e. Markers.append(..).and(..)
        LoggingEvent event = newEvent("msg", Markers.append("orderId", "1").and(Markers.append("orderId", "2")));

        assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_chainedMarkerUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg", Markers.append("orderId", "1").and(Markers.append("level", "CRITICAL")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    // --- Markers.appendEntries (MapEntriesAppendingMarker) ---

    @Test
    void validate_entriesWithUniqueKeys_doesNotThrow() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("field1", "value1");
        map.put("field2", "value2");
        LoggingEvent event = newEvent("msg", Markers.appendEntries(map));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_entriesUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg", Markers.appendEntries(Map.of("message", "duplicated")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_entriesCollidingWithMarkerAppend_throwsDuplicateField() {
        LoggingEvent event = newEvent("msg", Markers.append("orderId", "from-append"));
        event.addMarker(Markers.appendEntries(Map.of("orderId", "from-entries")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
    }

    @Test
    void validate_nestedMapValueReusingRootFieldName_doesNotThrow() {
        // fields nested inside a map value may re-use a name already used at the root - this is exactly
        // the documented workaround for avoiding root-level collisions.
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("myApp", Map.of("message", "not a duplicate, nested"));

        LoggingEvent event = newEvent("msg", Markers.appendEntries(map));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    // --- values are not serialized, and deferred markers not resolved ---

    public static class UnserializablePojo {

        @SuppressWarnings("unused")
        public String getBoom() {
            throw new IllegalArgumentException("not serializable by a plain mapper");
        }
    }

    @Test
    void validate_entryValues_areNotSerialized() {
        LoggingEvent event = newEvent("msg", Markers.appendEntries(Map.of("pojo", new UnserializablePojo())));

        // would throw if the value were serialized by the plain mapper
        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_entriesAfterEntryWithPojoValue_areStillValidated() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pojo", new UnserializablePojo());
        map.put("level", "CRITICAL");
        LoggingEvent event = newEvent("msg", Markers.appendEntries(map));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_deferredMarker_isNotResolved() {
        AtomicBoolean resolved = new AtomicBoolean();
        LoggingEvent event = newEvent("msg", Markers.defer(() -> {
            resolved.set(true);
            return Markers.append("message", "oops");
        }));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
        assertThat(resolved.get()).isFalse();
    }

    // --- Markers.appendFields (properties of an unwrapped object) ---

    public static class MessagePojo {

        public String getMessage() {
            return "oops";
        }
    }

    public static class OrderPojo {

        public String getOrderId() {
            return "from-pojo";
        }
    }

    @Test
    void validate_appendFieldsWithPropertyUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg", Markers.appendFields(new MessagePojo()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
        assertThat(exception).hasMessageThat().contains("message");
    }

    @Test
    void validate_appendFieldsWithPropertyCollidingWithMarkerAppend_throwsDuplicateField() {
        LoggingEvent event = newEvent("msg", Markers.appendFields(new OrderPojo()));
        event.addMarker(Markers.append("orderId", "from-append"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
    }

    @Test
    void validate_appendFieldsWithUniqueProperty_doesNotThrow() {
        LoggingEvent event = newEvent("msg", Markers.appendFields(new OrderPojo()));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_appendFieldsWhichCannotBeSerializedForInspection_doesNotThrow() {
        // the application's own mapper may well be able to serialize this
        LoggingEvent event = newEvent("msg", Markers.appendFields(new UnserializablePojo()));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_markerAfterOneWhichCannotBeSerializedForInspection_isStillValidated() {
        LoggingEvent event = newEvent("msg", Markers.appendFields(new UnserializablePojo()));
        event.addMarker(Markers.appendEntries(Map.of("level", "CRITICAL")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    // --- application-defined LogstashMarker subclasses ---

    /** A minimal custom marker, as an application might define, which writes with the given name. */
    private static final class CustomFieldMarker extends LogstashMarker {

        private final String fieldName;
        private final boolean serializableString;

        CustomFieldMarker(String fieldName, boolean serializableString) {
            super("CUSTOM_FIELD");
            this.fieldName = fieldName;
            this.serializableString = serializableString;
        }

        @Override
        public void writeTo(JsonGenerator generator) throws JacksonException {
            if (serializableString) {
                // as Jackson's bean serializers do
                generator.writeName(new SerializedString(fieldName));
                generator.writeString("value");
            } else {
                generator.writeStringProperty(fieldName, "value");
            }
        }
    }

    @Test
    void validate_customMarkerUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg", new CustomFieldMarker("message", false));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_customMarkerWritingSerializableStringUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg", new CustomFieldMarker("message", true));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_customMarkerCollidingWithMarkerAppend_throwsDuplicateField() {
        LoggingEvent event = newEvent("msg", new CustomFieldMarker("orderId", true));
        event.addMarker(Markers.append("orderId", "from-append"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
    }

    @Test
    void validate_customMarkerWithUniqueFieldName_doesNotThrow() {
        LoggingEvent event = newEvent("msg", new CustomFieldMarker("orderId", false));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    // --- what the Azure encoder does not write as root-level fields ---

    @Test
    void validate_mdcKeysUsingReservedOrMarkerFieldNames_doesNotThrow() {
        // the encoder nests the MDC in an 'mdc' object, so none of this is at the root
        LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO, "msg", null, null);
        event.setMDCPropertyMap(Map.of("message", "nested, so not reserved", "orderId", "nested, so not a duplicate"));
        event.addMarker(Markers.append("orderId", "at the root"));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_structuredArgumentsUsingReservedOrRepeatedFieldNames_doesNotThrow() {
        // the encoder has no arguments provider: structured arguments only end up in the message
        LoggingEvent event = newEvent("msg {} {} {}", null,
                StructuredArguments.kv("message", "oops"),
                StructuredArguments.kv("orderId", "1"),
                StructuredArguments.kv("orderId", "2"));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_keyValuePairsUsingReservedFieldName_doesNotThrow() {
        // the encoder has no key/value pairs provider either
        LoggingEvent event = newEvent("msg", null);
        event.setKeyValuePairs(List.of(new KeyValuePair("message", "oops")));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_noMarker_doesNotThrow() {
        LoggingEvent event = newEvent("msg", null);

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_azureValidatorDisabled_doesNotThrow() {
        AzureLogStatementFieldNameValidator validator = new AzureLogStatementFieldNameValidator();
        LoggingEvent event = newEvent("msg", Markers.append("message", "oops"));

        assertThrows(IllegalStateException.class, () -> validator.validate(event));

        validator.setEnabled(false);
        assertDoesNotThrow(() -> validator.validate(event));
    }
}
