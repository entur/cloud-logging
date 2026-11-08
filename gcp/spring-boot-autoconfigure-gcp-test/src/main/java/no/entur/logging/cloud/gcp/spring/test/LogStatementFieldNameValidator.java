package no.entur.logging.cloud.gcp.spring.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;
import net.logstash.logback.argument.DeferredStructuredArgument;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.marker.DeferredLogstashMarker;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverLogSeverityJsonProvider;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverLogstashEncoder;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverOpenTelemetryTraceMdcJsonProvider;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverServiceContextJsonProvider;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * places a mistake can actually originate from: the event's MDC, the logback context properties (which the
 * encoder also writes inline at the root), its SLF4J key/value pairs, any
 * {@link net.logstash.logback.marker.LogstashMarker} (including application-defined subclasses, not just the
 * built-in ones from {@code Markers}), and any {@link StructuredArgument} (from {@code StructuredArguments})
 * passed as a log argument. This keeps the check both fast (bounded by the number of fields an application
 * actually adds, not the size of the whole log statement) and precise (no false positives from unrelated
 * built-in content).
 *
 * <p>Markers and structured arguments are replayed against a discarding generator, so only the names
 * they write are looked at: values passed via {@code writePOJO} are never serialized, and
 * {@link DeferredStructuredArgument}/{@link DeferredLogstashMarker} are skipped altogether, as resolving
 * them would run the application's supplier on the calling thread rather than lazily in the encoder.
 * The properties of an object unwrapped by {@code fields}/{@code appendFields} are discovered by serializing
 * it with a plain, unconfigured mapper; if that fails, the element is skipped rather than failing the log
 * statement, as the application's own mapper may well serialize it fine.
 *
 * @see <a href="https://cloud.google.com/logging/docs/agent/logging/configuration#special-fields">Special fields in structured payloads</a>
 */
public final class LogStatementFieldNameValidator {

    /**
     * Field names always written by {@link StackdriverLogstashEncoder}'s built-in providers (see
     * {@link StackdriverLogSeverityJsonProvider}, {@code StackdriverMessageJsonProvider},
     * {@code StackdriverTimestampJsonProvider}, {@link StackdriverServiceContextJsonProvider}), plus the
     * remaining default logstash-logback-encoder fields. Using one of these names yourself - including as
     * a raw MDC key - is blindly forbidden, regardless of whether the corresponding provider would
     * actually end up writing it in a given log statement.
     */
    public static final Set<String> RESERVED_FIELD_NAMES = Set.of(
            "@version",
            "@timestamp",
            "logger_name",
            "thread_name",
            StackdriverLogSeverityJsonProvider.FIELD_SEVERITY,
            "message",
            "timestamp",
            StackdriverServiceContextJsonProvider.SERVICE_CONTEXT
    );

    /**
     * The GCP tracing field names, written by {@link StackdriverOpenTelemetryTraceMdcJsonProvider}/
     * {@code StackdriverMicrometerTraceMdcJsonProvider} when remapping another MDC key (e.g. {@code
     * traceId}/{@code trace_id}) - but also the officially supported way to set these fields directly,
     * used e.g. by {@code GcpTraceFilter}/{@code GcpTraceMdcSupport} and the gRPC trace interceptors,
     * which put the MDC entry under the literal GCP field name themselves so that the (unremapping)
     * default MDC provider writes it straight through. Since using one of these names as a raw MDC key is
     * therefore a legitimate, supported mechanism - not a mistake - they are only forbidden here when
     * written via a Marker or structured argument, which have no such legitimate use for them.
     */
    public static final Set<String> GCP_TRACE_FIELD_NAMES = Set.of(
            StackdriverOpenTelemetryTraceMdcJsonProvider.GCP_TRACE_KEY,
            StackdriverOpenTelemetryTraceMdcJsonProvider.GCP_SPAN_ID_KEY,
            StackdriverOpenTelemetryTraceMdcJsonProvider.GCP_TRACE_SAMPLED
    );

    /**
     * Field names forbidden when contributed via a Marker or structured argument: {@link
     * #RESERVED_FIELD_NAMES} plus {@link #GCP_TRACE_FIELD_NAMES} (see there for why the latter aren't
     * also forbidden as a raw MDC key).
     */
    private static final Set<String> MARKER_OR_ARGUMENT_RESERVED_FIELD_NAMES = Stream.concat(
            RESERVED_FIELD_NAMES.stream(), GCP_TRACE_FIELD_NAMES.stream()
    ).collect(Collectors.toUnmodifiableSet());

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private LogStatementFieldNameValidator() {
    }

    /**
     * @param event the log statement to validate
     * @throws IllegalStateException if the event would produce a repeated/reserved jsonPayload field name
     */
    public static void validate(ILoggingEvent event) {
        // the encoder's ContextJsonProvider writes the logback context properties inline at the root too
        Set<String> contextPropertyNames = contextPropertyNames(event);

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            // MDC keys come from a Map, so they are inherently unique among themselves - only the reserved
            // check (and the context properties) apply here, and neither needs a field-name set at all. GCP
            // trace field names are deliberately not checked against here - see GCP_TRACE_FIELD_NAMES.
            for (String key : mdc.keySet()) {
                checkReserved(key, RESERVED_FIELD_NAMES);
                if (contextPropertyNames.contains(key)) {
                    throw duplicateFieldException(key);
                }
            }
        }

        List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
        List<Marker> markers = event.getMarkerList();
        Object[] arguments = event.getArgumentArray();
        if (!mayContributeFields(keyValuePairs, markers, arguments)) {
            // by far the most common case: a log statement with no key/value pair, Marker or structured
            // argument contributes nothing beyond MDC, so skip allocating rootFieldNames/a JsonGenerator entirely.
            return;
        }

        // rootFieldNames (and fieldNamesPerLevel) are only created once something which can contribute fields
        // is known to be present, seeded with the MDC keys and context properties so they still participate in
        // the duplicate checks below (against key/value pairs, markers and arguments).
        Set<String> rootFieldNames = new HashSet<>(contextPropertyNames);
        if (mdc != null) {
            rootFieldNames.addAll(mdc.keySet());
        }
        Deque<Set<String>> fieldNamesPerLevel = new ArrayDeque<>();
        fieldNamesPerLevel.push(rootFieldNames);

        // A real (but discarding) generator is only allocated lazily, the first time it is actually
        // needed to safely replay a known structured element which writes its fields via generator calls
        // (e.g. map entries) - most log statements either have nothing to replay, or only elements handled
        // by the single-field fast path below, and never need it at all.
        try (LazyFieldNameCollectingJsonGenerator lazyCollector = new LazyFieldNameCollectingJsonGenerator(fieldNamesPerLevel)) {
            if (keyValuePairs != null) {
                for (KeyValuePair keyValuePair : keyValuePairs) {
                    // mirrors KeyValuePairsJsonProvider, which skips pairs lacking a key or a value
                    if (keyValuePair.key != null && keyValuePair.value != null) {
                        checkAndAdd(fieldNamesPerLevel, keyValuePair.key);
                    }
                }
            }

            if (markers != null) {
                for (Marker marker : markers) {
                    collectFromMarker(marker, lazyCollector, fieldNamesPerLevel);
                }
            }

            if (arguments != null) {
                for (Object argument : arguments) {
                    // mirrors ArgumentsJsonProvider, which only writes StructuredArgument arguments as fields
                    if (argument instanceof StructuredArgument structuredArgument) {
                        collect(structuredArgument, lazyCollector, fieldNamesPerLevel);
                    }
                }
            }
        }
    }

    private static Set<String> contextPropertyNames(ILoggingEvent event) {
        LoggerContextVO loggerContext = event.getLoggerContextVO();
        Map<String, String> properties = loggerContext != null ? loggerContext.getPropertyMap() : null;
        // not Set.of(): its contains(null) throws, and MDC keys may be null
        return properties != null ? properties.keySet() : Collections.emptySet();
    }

    private static boolean mayContributeFields(List<KeyValuePair> keyValuePairs, List<Marker> markers, Object[] arguments) {
        if (keyValuePairs != null && !keyValuePairs.isEmpty()) {
            return true;
        }
        if (markers != null && !markers.isEmpty()) {
            return true;
        }
        if (arguments != null) {
            for (Object argument : arguments) {
                if (argument instanceof StructuredArgument) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void collectFromMarker(Marker marker, LazyFieldNameCollectingJsonGenerator lazyCollector, Deque<Set<String>> fieldNamesPerLevel) {
        if (marker == null) {
            return;
        }
        // mirrors LogstashMarkersJsonProvider, which only writes LogstashMarkers (and, recursively, their references)
        if (marker instanceof LogstashMarker logstashMarker) {
            collect(logstashMarker, lazyCollector, fieldNamesPerLevel);
        }
        if (marker.hasReferences()) {
            Iterator<Marker> it = marker.iterator();
            while (it.hasNext()) {
                collectFromMarker(it.next(), lazyCollector, fieldNamesPerLevel);
            }
        }
    }

    /**
     * @param element a {@link LogstashMarker} and/or {@link StructuredArgument} which the encoder would write
     */
    private static void collect(Object element, LazyFieldNameCollectingJsonGenerator lazyCollector, Deque<Set<String>> fieldNamesPerLevel) {
        if (element instanceof DeferredStructuredArgument || element instanceof DeferredLogstashMarker) {
            // resolved lazily by the encoder; resolving it here would run the application's supplier eagerly,
            // on the calling thread.
            return;
        }

        if (element instanceof SingleFieldAppendingMarker single) {
            // known, common case (StructuredArguments.kv/value/keyValue, Markers.append): a single named
            // field - fast path, no need to involve the generator at all.
            checkAndAdd(fieldNamesPerLevel, single.getFieldName());
        } else if (element instanceof StructuredArgument structuredArgument) {
            // other known structured elements (e.g. Markers.appendEntries/StructuredArguments.entries/fields)
            // are replayed against the discarding generator to discover the field names they contribute.
            lazyCollector.replay(structuredArgument::writeTo);
        } else if (element instanceof LogstashMarker logstashMarker) {
            // any other LogstashMarker subclass (including application-defined ones, not just the built-in
            // ones which happen to also implement StructuredArgument) always exposes writeTo(JsonGenerator)
            // - safe to replay here for the same reason as above.
            lazyCollector.replay(logstashMarker::writeTo);
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
            checkReserved(fieldName, MARKER_OR_ARGUMENT_RESERVED_FIELD_NAMES);
        }

        Set<String> siblingFieldNames = fieldNamesPerLevel.peek();
        if (siblingFieldNames != null && !siblingFieldNames.add(fieldName)) {
            throw duplicateFieldException(fieldName);
        }
    }

    private static void checkReserved(String fieldName, Set<String> reservedFieldNames) {
        // null: the encoder skips nameless fields, and Set.of(..).contains(null) would throw
        if (fieldName != null && reservedFieldNames.contains(fieldName)) {
            throw new FieldNameViolationException(
                    "Forbidden JSON field '" + fieldName + "' detected in a log statement (added via MDC, a Marker "
                            + "or a structured argument). This field name is reserved by StackdriverLogstashEncoder's "
                            + "built-in providers; re-using it yourself will duplicate it in the jsonPayload, which "
                            + "Fluentbit cannot handle correctly. Rename the field, or nest it in a sub-object instead."
            );
        }
    }

    private static IllegalStateException duplicateFieldException(String fieldName) {
        return new FieldNameViolationException(
                "Duplicate JSON field '" + fieldName + "' detected in a log statement (added via MDC, a Marker, "
                        + "a structured argument, a key/value pair or a logback context property). Fluentbit cannot "
                        + "handle repeated jsonPayload field names correctly: this can corrupt values or cause "
                        + "several seconds of logs to be dropped. "
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

        /**
         * Replays a structured element against the discarding generator, so that the names it writes are checked.
         *
         * <p>Failing to serialize the element for inspection - say an unwrapped object whose getter throws, or
         * which needs modules only the application's own mapper has registered - is not a field name problem,
         * so that element is skipped (best effort), rather than failing the log statement. A violation found
         * along the way always propagates, though, even when Jackson has wrapped it on its way out of a
         * serializer.
         */
        void replay(Consumer<JsonGenerator> writeTo) {
            try {
                writeTo.accept(get());
            } catch (JacksonException e) {
                for (Throwable t = e; t != null; t = t.getCause()) {
                    if (t instanceof FieldNameViolationException violation) {
                        throw violation;
                    }
                }
                reset();
            }
        }

        private JsonGenerator get() {
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

        /**
         * Drops the generator, which is left in an unusable state when an element fails halfway through
         * writing, and unwinds back to the root level so that the next element starts afresh.
         */
        private void reset() {
            close();
            discard = null;
            collector = null;
            while (fieldNamesPerLevel.size() > 1) {
                fieldNamesPerLevel.pop();
            }
        }

        @Override
        public void close() {
            if (discard != null) {
                try {
                    discard.close();
                } catch (JacksonException e) {
                    // nothing was ever written anywhere; a generator which cannot be closed cleanly is of no interest
                }
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

        /**
         * Used by e.g. Jackson's bean serializers, so this is how the properties of an object unwrapped by
         * {@code fields}/{@code appendFields} are written.
         */
        @Override
        public JsonGenerator writeName(SerializableString name) throws JacksonException {
            checkAndAdd(fieldNamesPerLevel, name.getValue());
            return super.writeName(name);
        }

        @Override
        public JsonGenerator writePropertyId(long id) throws JacksonException {
            checkAndAdd(fieldNamesPerLevel, Long.toString(id));
            return super.writePropertyId(id);
        }

        /**
         * Only the names are of interest, so rather than serializing the value (with a mapper which is not the
         * application's own, and may not be able to), a placeholder is written in its place. This also covers
         * {@code writePOJOProperty}, which is a name followed by this.
         */
        @Override
        public JsonGenerator writePOJO(Object pojo) throws JacksonException {
            return super.writeNull();
        }
    }

    /**
     * Thrown for a reserved or repeated field name. A dedicated type so that it can be told apart from
     * unrelated failures, and found again when Jackson has wrapped it.
     */
    private static final class FieldNameViolationException extends IllegalStateException {

        private FieldNameViolationException(String message) {
            super(message);
        }
    }
}
