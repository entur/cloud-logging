package no.entur.logging.cloud.gcp.spring.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import no.entur.logging.cloud.appender.MdcContributer;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAsyncAppenderLogging;
import net.logstash.logback.argument.StructuredArguments;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.io.SerializedString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private LoggingEvent newEventWithKeyValuePairs(String... keysAndValues) {
        return newEventWithKeyValuePairs(Map.of(), keysAndValues);
    }

    private LoggingEvent newEventWithKeyValuePairs(Map<String, String> mdc, String... keysAndValues) {
        LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO, "a message", null, null);
        List<KeyValuePair> keyValuePairs = new ArrayList<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            keyValuePairs.add(new KeyValuePair(keysAndValues[i], keysAndValues[i + 1]));
        }
        event.setKeyValuePairs(keyValuePairs);
        event.setMDCPropertyMap(mdc);
        return event;
    }

    /** Not a started appender chain, but enough for {@code doAppend} to preprocess and validate an event. */
    private CompositeConsoleAsyncAppenderLogging newStartedAppender(MdcContributer mdcContributer) {
        CompositeConsoleAsyncAppenderLogging appender = new CompositeConsoleAsyncAppenderLogging();
        appender.setContext(context);
        appender.setName("TEST");
        appender.addAppender(new AppenderBase<ILoggingEvent>() {
            {
                setContext(context);
                start();
            }

            @Override
            protected void append(ILoggingEvent eventObject) {
            }
        });
        appender.setMdcContributer(mdcContributer);
        appender.setValidator(new GcpLogStatementFieldNameValidator());
        appender.start();
        return appender;
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
    void validate_nullMdcKey_doesNotThrow() {
        Map<String, String> mdc = new HashMap<>();
        mdc.put(null, "value");
        LoggingEvent event = newEvent("msg {}", null, mdc, StructuredArguments.kv("orderId", "123"));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_mdcContributedByAppender_isValidatedAfterBeingSnapshotted() {
        // e.g. the gRPC MDC, which is not on the thread's own MDC, but contributed when the appender preprocesses the event
        CompositeConsoleAsyncAppenderLogging appender = newStartedAppender(new MdcContributer() {
            @Override
            public Map<String, String> getMdc() {
                return Map.of("message", "oops");
            }
        });

        try {
            Logger realLogger = (Logger) LoggerFactory.getLogger(LogStatementFieldNameValidatorTest.class);
            LoggingEvent event = new LoggingEvent("fqcn", realLogger, Level.INFO, "msg", null, null);

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> appender.doAppend(event));
            assertThat(exception).hasMessageThat().contains("Forbidden");
        } finally {
            appender.stop();
        }
    }

    @Test
    void validate_gcpValidatorDisabled_doesNotThrow() {
        GcpLogStatementFieldNameValidator validator = new GcpLogStatementFieldNameValidator();
        LoggingEvent event = newEventWithMdc(Map.of("message", "oops"));

        assertThrows(IllegalStateException.class, () -> validator.validate(event));

        validator.setEnabled(false);
        assertDoesNotThrow(() -> validator.validate(event));
    }

    // --- SLF4J key/value pairs (logger.atInfo().addKeyValue(..)) ---

    @Test
    void validate_keyValuePairUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEventWithKeyValuePairs("severity", "CRITICAL");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
        assertThat(exception).hasMessageThat().contains("severity");
    }

    @Test
    void validate_keyValuePairCollidingWithMdc_throwsDuplicateField() {
        LoggingEvent event = newEventWithKeyValuePairs(Map.of("correlationId", "from-mdc"), "correlationId", "from-key-value");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
    }

    @Test
    void validate_keyValuePairCollidingWithStructuredArgument_throwsDuplicateField() {
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.kv("orderId", "from-argument"));
        event.setKeyValuePairs(List.of(new KeyValuePair("orderId", "from-key-value")));

        assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_twoKeyValuePairsWithSameKey_throwsDuplicateField() {
        LoggingEvent event = newEventWithKeyValuePairs("orderId", "1", "orderId", "2");

        assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_uniqueKeyValuePairs_doesNotThrow() {
        LoggingEvent event = newEventWithKeyValuePairs("orderId", "1", "customerId", "2");

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_keyValuePairWithoutValue_isIgnoredAsTheEncoderDoes() {
        LoggingEvent event = newEventWithKeyValuePairs("severity", null);

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
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

    // --- field names written as SerializableString (e.g. by Jackson's bean serializers) ---

    /** Written with {@code writeName(SerializableString)}, as Jackson's bean serializers do. */
    private static final class SerializableStringFieldMarker extends LogstashMarker {

        private final String fieldName;

        SerializableStringFieldMarker(String fieldName) {
            super("SERIALIZABLE_STRING_FIELD");
            this.fieldName = fieldName;
        }

        @Override
        public void writeTo(JsonGenerator generator) throws JacksonException {
            generator.writeName(new SerializedString(fieldName));
            generator.writeString("value");
        }
    }

    @Test
    void validate_serializableStringFieldNameUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg", new SerializableStringFieldMarker("message"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_serializableStringFieldNameCollidingWithStructuredArgument_throwsDuplicateField() {
        LoggingEvent event = newEvent("msg {}", new SerializableStringFieldMarker("orderId"), StructuredArguments.kv("orderId", "from-argument"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
    }

    // --- StructuredArguments.fields / Markers.appendFields (properties of an unwrapped object) ---

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
    void validate_fieldsWithPropertyUsingReservedFieldName_throwsForbiddenField() {
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.fields(new MessagePojo()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
        assertThat(exception).hasMessageThat().contains("message");
    }

    @Test
    void validate_appendFieldsWithPropertyCollidingWithStructuredArgument_throwsDuplicateField() {
        LoggingEvent event = newEvent("msg {}", Markers.appendFields(new OrderPojo()), StructuredArguments.kv("orderId", "from-argument"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Duplicate");
    }

    @Test
    void validate_fieldsWithUniqueProperty_doesNotThrow() {
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.fields(new OrderPojo()));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    public static class UnserializablePojo {

        @SuppressWarnings("unused")
        public String getBoom() {
            throw new IllegalArgumentException("not serializable by a plain mapper");
        }
    }

    @Test
    void validate_fieldsWhichCannotBeSerializedForInspection_doesNotThrow() {
        // the application's own mapper may well be able to serialize this
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.fields(new UnserializablePojo()));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_elementAfterOneWhichCannotBeSerializedForInspection_isStillValidated() {
        LoggingEvent event = newEvent("msg {} {}", null,
                StructuredArguments.fields(new UnserializablePojo()),
                StructuredArguments.entries(Map.of("severity", "CRITICAL")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    // --- values are not serialized, and deferred elements not resolved ---

    @Test
    void validate_entryValues_areNotSerialized() {
        UnserializablePojo value = new UnserializablePojo();
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.entries(Map.of("pojo", value)));

        // would throw if the value were serialized by the plain mapper
        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_entriesAfterEntryWithPojoValue_areStillValidated() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pojo", new UnserializablePojo());
        map.put("severity", "CRITICAL");
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.entries(map));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
        assertThat(exception).hasMessageThat().contains("Forbidden");
    }

    @Test
    void validate_deferredStructuredArgument_isNotResolved() {
        AtomicBoolean resolved = new AtomicBoolean();
        LoggingEvent event = newEvent("msg {}", null, StructuredArguments.defer(() -> {
            resolved.set(true);
            return StructuredArguments.kv("message", "oops");
        }));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
        assertThat(resolved.get()).isFalse();
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

    // --- what the encoder does not write as fields ---

    @Test
    void validate_logstashMarkerPassedAsArgument_isNotWrittenAsFieldByEncoder() {
        // ArgumentsJsonProvider only writes StructuredArguments, so this is just a message argument
        LoggingEvent event = newEvent("msg {}", null, new CustomFieldMarker("message", "oops"));

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_plainArgumentsBeforeStructuredArgument_stillValidated() {
        LoggingEvent event = newEvent("msg {} {} {}", null, "plain", 42, StructuredArguments.kv("severity", "CRITICAL"));

        assertThrows(IllegalStateException.class, () -> LogStatementFieldNameValidator.validate(event));
    }

    @Test
    void validate_plainArgumentsOnly_doesNotThrow() {
        LoggingEvent event = newEvent("msg {} {}", null, Map.of(), Map.of("message", "plain argument, not a field"), "plain");

        assertDoesNotThrow(() -> LogStatementFieldNameValidator.validate(event));
    }
}
