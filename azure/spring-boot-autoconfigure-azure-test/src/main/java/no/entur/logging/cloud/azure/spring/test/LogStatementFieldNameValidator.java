package no.entur.logging.cloud.azure.spring.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.marker.DeferredLogstashMarker;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import no.entur.logging.cloud.azure.logback.logstash.AzureServiceContextJsonProvider;
import org.slf4j.Marker;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Validates that a log statement does not contribute a root-level JSON field name which is either
 * already reserved by one of the built-in providers of the Azure encoder, or repeated.
 *
 * <p>A JSON object with the same field name twice is ambiguous: parsers disagree on which value wins, so
 * the log entry ends up with a value other than the one intended, or is rejected, once ingested by Azure
 * Monitor.
 *
 * <p>This mirrors what the Azure encoder (see {@code logback-spring-test.xml}, and
 * {@code logback-default-server.xml} for production) actually writes at the root of a log statement, and
 * only looks at the places a mistake can originate from, rather than the fully rendered JSON:
 * <ul>
 *     <li>{@link LogstashMarker}s (including application-defined subclasses, not just the built-in ones from
 *     {@code Markers}), whose fields are written at the root.</li>
 * </ul>
 * and, deliberately, not at:
 * <ul>
 *     <li>the MDC, which the encoder nests in an {@code mdc} object, so its keys cannot collide with
 *     anything at the root.</li>
 *     <li>structured arguments (from {@code StructuredArguments}) and SLF4J key/value pairs, which the encoder does
 *     not write as fields at all, only as part of the message.</li>
 * </ul>
 *
 * <p>Markers are replayed against a discarding generator, so only the names they write are looked at:
 * values passed via {@code writePOJO} are never serialized, and {@link DeferredLogstashMarker}s are skipped
 * altogether, as resolving them would run the application's supplier on the calling thread rather than lazily
 * in the encoder. The properties of an object unwrapped by {@code appendFields} are discovered by serializing
 * it with a plain, unconfigured mapper; if that fails, the marker is skipped rather than failing the log
 * statement, as the application's own mapper may well serialize it fine.
 *
 * @see <a href="https://learn.microsoft.com/en-us/azure/azure-monitor/logs/log-standard-columns">Azure Monitor log standard columns</a>
 */
public final class LogStatementFieldNameValidator {

    /**
     * Field names written at the root by the Azure encoder's built-in providers: those configured in
     * {@code logback-spring-test.xml}/{@code logback-default-server.xml} (timestamp, logger name, level,
     * thread name, the nested {@code mdc} object, message, and the stacktrace and exception class), plus the
     * {@link AzureServiceContextJsonProvider service context} which {@code AzureLogstashEncoder} adds.
     * Using one of these names yourself is blindly forbidden, regardless of whether the corresponding provider
     * would actually end up writing it in a given log statement.
     */
    public static final Set<String> RESERVED_FIELD_NAMES = Set.of(
            "timestamp",
            "logger",
            "level",
            "thread",
            "mdc",
            "message",
            "stackTrace",
            "exceptionClass",
            AzureServiceContextJsonProvider.SERVICE_CONTEXT
    );

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private LogStatementFieldNameValidator() {
    }

    /**
     * @param event the log statement to validate
     * @throws IllegalStateException if the event would produce a repeated/reserved root-level JSON field name
     */
    public static void validate(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        if (markers == null || markers.isEmpty()) {
            // by far the most common case: a log statement without a Marker contributes no root-level
            // fields besides the built-in ones, so skip allocating the field-name sets/a JsonGenerator entirely.
            return;
        }

        Deque<Set<String>> fieldNamesPerLevel = new ArrayDeque<>();
        fieldNamesPerLevel.push(new HashSet<>());

        // A real (but discarding) generator is only allocated lazily, the first time it is actually
        // needed to safely replay a marker which writes its fields via generator calls (e.g. map entries)
        // - most markers are handled by the single-field fast path below, and never need it at all.
        try (LazyFieldNameCollectingJsonGenerator lazyCollector = new LazyFieldNameCollectingJsonGenerator(fieldNamesPerLevel)) {
            for (Marker marker : markers) {
                collectFromMarker(marker, lazyCollector, fieldNamesPerLevel);
            }
        }
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

    private static void collect(LogstashMarker marker, LazyFieldNameCollectingJsonGenerator lazyCollector, Deque<Set<String>> fieldNamesPerLevel) {
        if (marker instanceof DeferredLogstashMarker) {
            // resolved lazily by the encoder; resolving it here would run the application's supplier eagerly,
            // on the calling thread.
            return;
        }

        if (marker instanceof SingleFieldAppendingMarker single) {
            // known, common case (Markers.append): a single named field - fast path, no need to involve
            // the generator at all.
            checkAndAdd(fieldNamesPerLevel, single.getFieldName());
        } else {
            // any other LogstashMarker (e.g. Markers.appendEntries/appendFields, or an application-defined one)
            // always exposes writeTo(JsonGenerator) - replayed against the discarding generator to discover
            // the field names it contributes.
            lazyCollector.replay(marker::writeTo);
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

        // Reserved names are only ever written by the encoder's built-in providers at the root of the log
        // statement (stack size 1 == the object opened by validate() itself), so re-using one of them in a
        // nested sub-object is fine and not flagged here.
        if (fieldNamesPerLevel.size() == 1 && RESERVED_FIELD_NAMES.contains(fieldName)) {
            throw new FieldNameViolationException(
                    "Forbidden JSON field '" + fieldName + "' detected in a log statement (added via a Marker). "
                            + "This field name is reserved by the Azure encoder's built-in providers; re-using it "
                            + "yourself will duplicate it in the JSON, which is ambiguous for consumers. "
                            + "Rename the field, or nest it in a sub-object instead."
            );
        }

        Set<String> siblingFieldNames = fieldNamesPerLevel.peek();
        if (siblingFieldNames != null && !siblingFieldNames.add(fieldName)) {
            throw new FieldNameViolationException(
                    "Duplicate JSON field '" + fieldName + "' detected in a log statement (added via a Marker). "
                            + "Repeated field names are ambiguous for consumers: which value is kept differs "
                            + "between parsers. Rename the offending key, or nest it in a sub-object instead."
            );
        }
    }

    /**
     * Lazily creates a real (but discarding) {@link JsonGenerator}, only on first use, wrapped so that
     * every field name written through it (including into nested objects) is tracked/checked the same
     * way as {@link #checkAndAdd}. Kept separate from the plain single-field markers above so that the
     * common case - no marker requiring a real generator at all - never pays for creating one.
     */
    private static final class LazyFieldNameCollectingJsonGenerator implements AutoCloseable {

        private final Deque<Set<String>> fieldNamesPerLevel;
        private JsonGenerator discard;
        private FieldNameCollectingJsonGenerator collector;

        private LazyFieldNameCollectingJsonGenerator(Deque<Set<String>> fieldNamesPerLevel) {
            this.fieldNamesPerLevel = fieldNamesPerLevel;
        }

        /**
         * Replays a marker against the discarding generator, so that the names it writes are checked.
         *
         * <p>Failing to serialize the marker for inspection - say an unwrapped object whose getter throws, or
         * which needs modules only the application's own mapper has registered - is not a field name problem,
         * so that marker is skipped (best effort), rather than failing the log statement. A violation found
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
         * Drops the generator, which is left in an unusable state when a marker fails halfway through
         * writing, and unwinds back to the root level so that the next marker starts afresh.
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
     * operation to a real generator so that markers can be replayed without error.
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
         * {@code appendFields} are written.
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
