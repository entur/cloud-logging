package no.entur.logging.cloud.gcp.spring.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverLogSeverityJsonProvider;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverLogstashEncoder;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverOpenTelemetryTraceMdcJsonProvider;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverServiceContextJsonProvider;
import org.slf4j.Marker;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates that a log statement does not contribute a jsonPayload field name which is either
 * already reserved by {@link StackdriverLogstashEncoder}'s built-in providers, or repeated.
 *
 * <p>Google Cloud Logging's Fluentbit forwarder cannot deal with a {@code jsonPayload} which contains
 * the same field name twice at the same level: this typically corrupts values, and can make Fluentbit
 * drop 2-4 seconds worth of logs from all pods (not just the offending one).
 *
 * <p>Rather than inspecting the fully rendered JSON (which would mean re-parsing/re-walking the whole
 * log statement, including its message, stacktrace, service context, etc.), this only looks at the
 * places a mistake can actually originate from: the event's MDC, any {@link net.logstash.logback.marker.LogstashMarker}
 * (including application-defined subclasses, not just the built-in ones from {@code Markers}), and any
 * known {@link StructuredArgument} type (from {@code StructuredArguments}) passed as a log argument.
 * This keeps the check both fast (bounded by the number of fields an application actually adds, not the
 * size of the whole log statement) and precise (no false positives from unrelated built-in content).
 *
 * @see <a href="https://cloud.google.com/logging/docs/agent/logging/configuration#special-fields">Special fields in structured payloads</a>
 */
public final class LogStatementFieldNameValidator {

    /**
     * Field names always written by {@link StackdriverLogstashEncoder}'s built-in providers (see
     * {@link StackdriverLogSeverityJsonProvider}, {@code StackdriverMessageJsonProvider},
     * {@code StackdriverTimestampJsonProvider}, {@link StackdriverServiceContextJsonProvider}), the GCP
     * tracing fields written by {@link StackdriverOpenTelemetryTraceMdcJsonProvider}/
     * {@code StackdriverMicrometerTraceMdcJsonProvider}, plus the remaining default
     * logstash-logback-encoder fields. Using one of these names yourself - including as a raw MDC key -
     * is blindly forbidden, regardless of whether the corresponding provider would actually end up
     * writing it in a given log statement.
     */
    public static final Set<String> RESERVED_FIELD_NAMES = Set.of(
            "@version",
            "@timestamp",
            "logger_name",
            "thread_name",
            StackdriverLogSeverityJsonProvider.FIELD_SEVERITY,
            "message",
            "timestamp",
            StackdriverServiceContextJsonProvider.SERVICE_CONTEXT,
            StackdriverOpenTelemetryTraceMdcJsonProvider.GCP_TRACE_KEY,
            StackdriverOpenTelemetryTraceMdcJsonProvider.GCP_SPAN_ID_KEY,
            StackdriverOpenTelemetryTraceMdcJsonProvider.GCP_TRACE_SAMPLED
    );

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private LogStatementFieldNameValidator() {
    }

    /**
     * @param event the log statement to validate
     * @throws IllegalStateException if the event would produce a repeated/reserved jsonPayload field name
     */
    public static void validate(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            // MDC keys come from a Map, so they are inherently unique among themselves - only the reserved
            // check applies here, and it needs no field-name set at all.
            for (String key : mdc.keySet()) {
                checkReserved(key);
            }
        }

        List<Marker> markers = event.getMarkerList();
        Object[] arguments = event.getArgumentArray();
        if ((markers == null || markers.isEmpty()) && (arguments == null || arguments.length == 0)) {
            // by far the most common case: a log statement with no Marker and no argument at all
            // contributes nothing beyond MDC, so skip allocating rootFieldNames/a JsonGenerator entirely.
            return;
        }

        // rootFieldNames (and fieldNamesPerLevel) are only created once markers/arguments are actually
        // known to be present, seeded with the MDC keys so they still participate in the duplicate checks
        // below (against markers/arguments).
        Set<String> rootFieldNames = mdc != null ? new HashSet<>(mdc.keySet()) : new HashSet<>();
        Deque<Set<String>> fieldNamesPerLevel = new ArrayDeque<>();
        fieldNamesPerLevel.push(rootFieldNames);

        // A real (but discarding) generator is only allocated lazily, the first time it is actually
        // needed to safely replay a known structured element which writes its fields via generator calls
        // (e.g. map entries) - most log statements either have no markers/arguments, or only ones handled
        // by the single-field fast path below, and never need it at all.
        try (LazyFieldNameCollectingJsonGenerator lazyCollector = new LazyFieldNameCollectingJsonGenerator(fieldNamesPerLevel)) {
            if (markers != null) {
                for (Marker marker : markers) {
                    collectFromMarker(marker, lazyCollector, fieldNamesPerLevel);
                }
            }

            if (arguments != null) {
                for (Object argument : arguments) {
                    collectFromMarkerOrArgument(argument, lazyCollector, fieldNamesPerLevel);
                }
            }
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to validate structured log fields for log statement", e);
        }
    }

    private static void collectFromMarker(Marker marker, LazyFieldNameCollectingJsonGenerator lazyCollector, Deque<Set<String>> fieldNamesPerLevel) {
        if (marker == null) {
            return;
        }
        collectFromMarkerOrArgument(marker, lazyCollector, fieldNamesPerLevel);
        if (marker.hasReferences()) {
            Iterator<Marker> it = marker.iterator();
            while (it.hasNext()) {
                collectFromMarker(it.next(), lazyCollector, fieldNamesPerLevel);
            }
        }
    }

    private static void collectFromMarkerOrArgument(Object markerOrArgument, LazyFieldNameCollectingJsonGenerator lazyCollector, Deque<Set<String>> fieldNamesPerLevel) {
        if (markerOrArgument instanceof SingleFieldAppendingMarker single) {
            // known, common case (StructuredArguments.kv/value/keyValue, Markers.append): a single named
            // field - fast path, no need to involve the generator at all.
            checkAndAdd(fieldNamesPerLevel, single.getFieldName());
        } else if (markerOrArgument instanceof StructuredArgument structuredArgument) {
            // other known structured elements (e.g. Markers.appendEntries/StructuredArguments.entries) only
            // use simple, context-free generator calls, so it is safe to replay them here to discover the
            // field names they contribute. Elements which unwrap an arbitrary object's own fields (e.g.
            // appendFields/StructuredArguments.fields) require a live Jackson SerializationContext that isn't
            // meaningfully available here, and are a no-op in that case (best effort only).
            structuredArgument.writeTo(lazyCollector.get());
        } else if (markerOrArgument instanceof LogstashMarker logstashMarker) {
            // any other LogstashMarker subclass (including application-defined ones, not just the built-in
            // ones which happen to also implement StructuredArgument) always exposes writeTo(JsonGenerator)
            // - safe to replay here for the same reason as above, with the same SerializationContext caveat.
            logstashMarker.writeTo(lazyCollector.get());
        }
    }

    /**
     * @param fieldNamesPerLevel the stack of field name sets, one per currently open JSON object level;
     *                           the top of the stack is the level {@code fieldName} is being added to
     * @param fieldName          the field name to check and record
     */
    private static void checkAndAdd(Deque<Set<String>> fieldNamesPerLevel, String fieldName) {
        if (fieldName == null) {
            return;
        }

        // Reserved names are only ever written by StackdriverLogstashEncoder's built-in providers at the
        // root of the log statement (stack size 1 == the object opened by validate() itself), so re-using
        // one of them in a nested sub-object is fine and not flagged here.
        if (fieldNamesPerLevel.size() == 1) {
            checkReserved(fieldName);
        }

        Set<String> siblingFieldNames = fieldNamesPerLevel.peek();
        if (siblingFieldNames != null && !siblingFieldNames.add(fieldName)) {
            throw duplicateFieldException(fieldName);
        }
    }

    private static void checkReserved(String fieldName) {
        if (RESERVED_FIELD_NAMES.contains(fieldName)) {
            throw new IllegalStateException(
                    "Forbidden JSON field '" + fieldName + "' detected in a log statement (added via MDC, a Marker "
                            + "or a structured argument). This field name is reserved by StackdriverLogstashEncoder's "
                            + "built-in providers; re-using it yourself will duplicate it in the jsonPayload, which "
                            + "Fluentbit cannot handle correctly. Rename the field, or nest it in a sub-object instead."
            );
        }
    }

    private static IllegalStateException duplicateFieldException(String fieldName) {
        return new IllegalStateException(
                "Duplicate JSON field '" + fieldName + "' detected in a log statement (added via MDC, a Marker or "
                        + "a structured argument). Fluentbit cannot handle repeated jsonPayload field names "
                        + "correctly: this can corrupt values or cause several seconds of logs to be dropped. "
                        + "Rename the offending key, or nest it in a sub-object instead."
        );
    }


    /**
     * Lazily creates a real (but discarding) {@link JsonGenerator}, only on first use, wrapped so that
     * every field name written through it (including into nested objects) is tracked/checked the same
     * way as {@link #checkAndAdd}. Kept separate from the plain root-level checks (MDC, single-field
     * markers/arguments) above so that the common case - no marker/argument requiring a real generator at
     * all - never pays for creating one.
     */
    private static final class LazyFieldNameCollectingJsonGenerator implements AutoCloseable {

        private final Deque<Set<String>> fieldNamesPerLevel;
        private JsonGenerator discard;
        private FieldNameCollectingJsonGenerator collector;

        private LazyFieldNameCollectingJsonGenerator(Deque<Set<String>> fieldNamesPerLevel) {
            this.fieldNamesPerLevel = fieldNamesPerLevel;
        }

        JsonGenerator get() {
            if (collector == null) {
                discard = MAPPER.createGenerator(OutputStream.nullOutputStream());
                // opened directly on the real generator (bypassing FieldNameCollectingJsonGenerator's own
                // writeStartObject override) since the root level is already accounted for in
                // fieldNamesPerLevel by validate() itself - opening it again here would double-count it.
                discard.writeStartObject();
                collector = new FieldNameCollectingJsonGenerator(discard, fieldNamesPerLevel);
            }
            return collector;
        }

        @Override
        public void close() {
            if (discard != null) {
                discard.close();
            }
        }
    }

    /**
     * Tracks the field names seen at the current JSON object level using a stack of sets, one per nesting
     * level (pushed on {@code writeStartObject}, popped on {@code writeEndObject}). Delegates every other
     * operation to a real generator so that known structured elements can be replayed without error.
     */
    private static final class FieldNameCollectingJsonGenerator extends JsonGeneratorDelegate {

        private final Deque<Set<String>> fieldNamesPerLevel;

        private FieldNameCollectingJsonGenerator(JsonGenerator generator, Deque<Set<String>> fieldNamesPerLevel) {
            super(generator, false);
            this.fieldNamesPerLevel = fieldNamesPerLevel;
        }

        @Override
        public JsonGenerator writeStartObject() throws JacksonException {
            fieldNamesPerLevel.push(new HashSet<>());
            return super.writeStartObject();
        }

        @Override
        public JsonGenerator writeStartObject(Object forValue) throws JacksonException {
            fieldNamesPerLevel.push(new HashSet<>());
            return super.writeStartObject(forValue);
        }

        @Override
        public JsonGenerator writeStartObject(Object forValue, int size) throws JacksonException {
            fieldNamesPerLevel.push(new HashSet<>());
            return super.writeStartObject(forValue, size);
        }

        @Override
        public JsonGenerator writeEndObject() throws JacksonException {
            fieldNamesPerLevel.pop();
            return super.writeEndObject();
        }

        @Override
        public JsonGenerator writeName(String name) throws JacksonException {
            checkAndAdd(fieldNamesPerLevel, name);
            return super.writeName(name);
        }
    }
}
