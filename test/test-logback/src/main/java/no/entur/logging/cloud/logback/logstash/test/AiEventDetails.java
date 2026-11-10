package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.spi.ContextAwareBase;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.loggingevent.KeyValuePairsJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventCompositeJsonFormatter;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;
import net.logstash.logback.marker.LogstashMarker;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The structured data of an event for the AI files, read from its JSON output: the fields of Logstash markers, SLF4J key-value pairs
 * and structured arguments which are not part of the message (no <code>{}</code> placeholder). A request-response marker (an
 * <code>http</code> object with <code>headers</code>, written by the HTTP and gRPC request-response markers) gives the headers and
 * body; an {@link AiBodyMarker} replaces the body. The other fields are kept as a JSON object; for the full file also the other members
 * of the request-response <code>http</code> object, such as <code>origin</code>, <code>protocol</code>, <code>remote</code> and the gRPC
 * <code>deadline-in</code>. If the JSON output is not valid JSON (for example a malformed JSON body from a local request, which the
 * request-response marker writes as is), it is kept whole as the other fields, so that headers and body are not lost.
 * <p>
 * Kept for the last event per thread, as the compact and the full file render the same event one after the other.
 */
public final class AiEventDetails {

    public record Header(String name, String value) {
    }

    static final AiEventDetails NONE = new AiEventDetails(false, Collections.emptyList(), null, false, null, null);

    private static final LoggingEventCompositeJsonFormatter FORMATTER = createFormatter();

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private static final ThreadLocal<Object[]> LAST = new ThreadLocal<>();

    private final boolean requestResponse;
    private final List<Header> headers;
    private final String body;
    private final boolean jsonBody;
    private final String fields;
    private final String fullFields;
    private boolean completeBody;
    private boolean validJsonBody;

    // computed once per event, as the event id, the compact file and the full file need them (the event is rendered on one thread)
    private String singleLineBody;
    private boolean singleLineBodyDone;
    private String compactBody;
    private boolean compactBodyDone;
    private String compactFields;
    private boolean compactFieldsDone;

    private AiEventDetails(boolean requestResponse, List<Header> headers, String body, boolean jsonBody, String fields, String fullFields) {
        this.requestResponse = requestResponse;
        this.headers = headers;
        this.body = body;
        this.jsonBody = jsonBody;
        this.fields = fields;
        this.fullFields = fullFields;
    }

    /** True if the event comes from request-response logging (HTTP or gRPC). */
    public boolean isRequestResponse() {
        return requestResponse;
    }

    /** Request-response headers, names in lower case. */
    public List<Header> getHeaders() {
        return headers;
    }

    /** Request-response body as logged (possibly on several lines), or null. */
    public String getBody() {
        return body;
    }

    public boolean isJsonBody() {
        return jsonBody;
    }

    /** True if the body is the whole body (from an {@link AiBodyMarker}), not the one in the JSON output, which may be cut. */
    public boolean isCompleteBody() {
        return completeBody;
    }

    /** True if the body is known to be valid JSON (it is the JSON body of the event's JSON output, which was parsed). */
    boolean isValidJsonBody() {
        return validJsonBody;
    }

    /** The body on a single line (see {@link AiText#singleLineBody(String, boolean)}), or null if there is none. */
    public String getSingleLineBody() {
        if (!singleLineBodyDone) {
            singleLineBody = body == null || body.isBlank() ? null : AiText.singleLineBody(body, jsonBody, validJsonBody);
            singleLineBodyDone = true;
        }
        return singleLineBody;
    }

    /** {@link #getSingleLineBody()} with UUIDs aliased for the compact file (not cut). */
    public String getCompactBody() {
        if (!compactBodyDone) {
            String singleLine = getSingleLineBody();
            compactBody = singleLine == null ? null : AiAliases.aliasUuids(singleLine, false);
            compactBodyDone = true;
        }
        return compactBody;
    }

    /** {@link #getFields()} with UUIDs aliased for the compact file (not cut). */
    public String getCompactFields() {
        if (!compactFieldsDone) {
            compactFields = fields == null ? null : AiAliases.aliasUuids(fields, false);
            compactFieldsDone = true;
        }
        return compactFields;
    }

    /** The other structured fields as a JSON object, or null if there are none. */
    public String getFields() {
        return fields;
    }

    /** Like {@link #getFields()}, plus the other members of the request-response <code>http</code> object, for the full file. */
    public String getFullFields() {
        return fullFields;
    }

    public static AiEventDetails of(ILoggingEvent event) {
        Object[] last = LAST.get();
        if (last != null && last[0] == event) {
            return (AiEventDetails) last[1];
        }
        AiEventDetails details = parse(event);
        LAST.set(new Object[]{event, details});
        return details;
    }

    static AiEventDetails parse(ILoggingEvent event) {
        if (!hasStructuredData(event)) {
            return NONE;
        }
        String json = toJson(event);
        if (json == null) {
            return NONE;
        }
        return parse(json, findBodyMarker(event.getMarkerList()));
    }

    static AiEventDetails parse(String json, AiBodyMarker bodyMarker) {
        try (JsonParser parser = JSON_FACTORY.createParser(ObjectReadContext.empty(), json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return NONE;
            }
            AiEventDetails http = null;
            List<String> fields = new ArrayList<>();
            List<String> fullFields = new ArrayList<>();
            JsonToken token = parser.nextToken();
            while (token == JsonToken.PROPERTY_NAME) {
                int start = offset(parser);
                String name = parser.currentName();
                JsonToken value = parser.nextToken();
                AiEventDetails parsed = null;
                if (http == null && "http".equals(name) && value == JsonToken.START_OBJECT) {
                    parsed = parseHttp(parser, json, bodyMarker);
                } else {
                    parser.skipChildren();
                }
                token = parser.nextToken();
                if (parsed != null) {
                    http = parsed;
                    if (parsed.fields != null) {
                        fullFields.add("\"http\":" + parsed.fields);
                    }
                } else {
                    String field = withoutTrailingComma(json.substring(start, offset(parser)));
                    fields.add(field);
                    fullFields.add(field);
                }
            }
            String otherFields = toObject(fields);
            String allFields = toObject(fullFields);
            if (http == null) {
                return otherFields == null ? NONE : new AiEventDetails(false, Collections.emptyList(), null, false, otherFields, allFields);
            }
            AiEventDetails details = new AiEventDetails(true, http.headers, http.body, http.jsonBody, otherFields, allFields);
            details.completeBody = http.completeBody;
            details.validJsonBody = http.validJsonBody;
            return details;
        } catch (JacksonException | IndexOutOfBoundsException e) {
            // not valid JSON: keep it whole
            String raw = AiText.singleLine(json.strip());
            return raw.isEmpty() ? NONE : new AiEventDetails(false, Collections.emptyList(), null, false, raw, raw);
        }
    }

    private static String toObject(List<String> fields) {
        return fields.isEmpty() ? null : AiText.singleLine("{" + String.join(",", fields) + "}");
    }

    /**
     * Parses an <code>http</code> object; null (after skipping it) if it has no headers, i.e. is not from request-response logging.
     * The members other than headers and body are returned as the fields.
     */
    private static AiEventDetails parseHttp(JsonParser parser, String json, AiBodyMarker bodyMarker) {
        List<Header> headers = null;
        String body = null;
        boolean jsonBody = false;
        List<String> rest = new ArrayList<>();
        JsonToken token = parser.nextToken();
        while (token == JsonToken.PROPERTY_NAME) {
            int nameStart = offset(parser);
            String name = parser.currentName();
            JsonToken value = parser.nextToken();
            int start = offset(parser);
            if ("headers".equals(name) && value == JsonToken.START_OBJECT) {
                headers = parseHeaders(parser, json);
                token = parser.nextToken();
            } else if ("body".equals(name) && value == JsonToken.VALUE_STRING) {
                body = parser.getString();
                jsonBody = false;
                token = parser.nextToken();
            } else if ("body".equals(name) && value != JsonToken.VALUE_NULL) {
                parser.skipChildren();
                token = parser.nextToken();
                body = withoutTrailingComma(json.substring(start, offset(parser)));
                jsonBody = true;
            } else {
                parser.skipChildren();
                token = parser.nextToken();
                rest.add(withoutTrailingComma(json.substring(nameStart, offset(parser))));
            }
        }
        if (headers == null) {
            return null;
        }
        boolean completeBody = false;
        // a JSON body written into the event's JSON has just been parsed, so it is valid JSON (no need to parse it again)
        boolean validJsonBody = jsonBody;
        if (bodyMarker != null && bodyMarker.getBody() != null) {
            String eventBody = jsonBody ? body : null;
            body = bodyMarker.getBody();
            jsonBody = bodyMarker.isJson();
            completeBody = true;
            validJsonBody = jsonBody && eventBody != null && eventBody.equals(body.strip());
        }
        AiEventDetails details = new AiEventDetails(true, headers, body, jsonBody, rest.isEmpty() ? null : "{" + String.join(",", rest) + "}", null);
        details.completeBody = completeBody;
        details.validJsonBody = validJsonBody;
        return details;
    }

    /** Header values are arrays of strings, or of JSON values (for example rich gRPC error details). */
    private static List<Header> parseHeaders(JsonParser parser, String json) {
        List<Header> headers = new ArrayList<>();
        while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
            String name = parser.currentName();
            JsonToken value = parser.nextToken();
            List<String> values = new ArrayList<>();
            if (value == JsonToken.START_ARRAY) {
                JsonToken element = parser.nextToken();
                while (element != JsonToken.END_ARRAY) {
                    int start = offset(parser);
                    if (element == JsonToken.VALUE_STRING) {
                        values.add(parser.getString());
                        element = parser.nextToken();
                    } else if (element == JsonToken.VALUE_NULL) {
                        element = parser.nextToken();
                    } else {
                        parser.skipChildren();
                        element = parser.nextToken();
                        values.add(AiText.singleLineBody(withoutTrailingComma(json.substring(start, offset(parser))), true));
                    }
                }
            } else if (value == JsonToken.VALUE_STRING) {
                values.add(parser.getString());
            } else {
                parser.skipChildren();
            }
            headers.add(new Header(name, String.join(", ", values)));
        }
        return headers;
    }

    private static int offset(JsonParser parser) {
        return (int) parser.currentTokenLocation().getCharOffset();
    }

    private static String withoutTrailingComma(String text) {
        String stripped = text.strip();
        return stripped.endsWith(",") ? stripped.substring(0, stripped.length() - 1).strip() : stripped;
    }

    private static boolean hasStructuredData(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        if (markers != null) {
            for (Marker marker : markers) {
                if (hasLogstashMarker(marker)) {
                    return true;
                }
            }
        }
        List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
        if (keyValuePairs != null && !keyValuePairs.isEmpty()) {
            return true;
        }
        Object[] arguments = event.getArgumentArray();
        if (arguments != null) {
            for (int i = placeholders(event.getMessage()); i < arguments.length; i++) {
                if (arguments[i] instanceof StructuredArgument) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasLogstashMarker(Marker marker) {
        if (marker instanceof LogstashMarker) {
            return true;
        }
        if (marker.hasReferences()) {
            Iterator<Marker> iterator = marker.iterator();
            while (iterator.hasNext()) {
                if (hasLogstashMarker(iterator.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    static AiBodyMarker findBodyMarker(List<Marker> markers) {
        if (markers != null) {
            for (Marker marker : markers) {
                AiBodyMarker found = findBodyMarker(marker);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static AiBodyMarker findBodyMarker(Marker marker) {
        if (marker instanceof AiBodyMarker bodyMarker) {
            return bodyMarker;
        }
        if (marker.hasReferences()) {
            Iterator<Marker> iterator = marker.iterator();
            while (iterator.hasNext()) {
                AiBodyMarker found = findBodyMarker(iterator.next());
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // reused per thread (events are written on the async appender's thread), so that a large body does not grow a new buffer each time
    private static final ThreadLocal<ByteArrayOutputStream> BUFFER = ThreadLocal.withInitial(() -> new ByteArrayOutputStream(4096));

    private static final int MAX_KEPT_BUFFER = 1024 * 1024;

    private static String toJson(ILoggingEvent event) {
        ByteArrayOutputStream out = BUFFER.get();
        out.reset();
        try {
            FORMATTER.writeEvent(event, out);
        } catch (IOException | RuntimeException e) {
            return null;
        }
        String json = out.toString(StandardCharsets.UTF_8).strip();
        if (out.size() > MAX_KEPT_BUFFER) {
            BUFFER.remove();
        }
        return json.isEmpty() || json.equals("{}") ? null : json;
    }

    /** Number of <code>{}</code> placeholders in an SLF4J message pattern (<code>\{}</code> is escaped, <code>\\{}</code> is not). */
    static int placeholders(String pattern) {
        if (pattern == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i + 1 < pattern.length(); i++) {
            if (pattern.charAt(i) == '{' && pattern.charAt(i + 1) == '}') {
                boolean escaped = i > 0 && pattern.charAt(i - 1) == '\\' && !(i > 1 && pattern.charAt(i - 2) == '\\');
                if (!escaped) {
                    count++;
                }
                i++;
            }
        }
        return count;
    }

    /** Structured arguments which are not part of the message, as the GCP and Azure encoders write them. */
    static class UnformattedStructuredArgumentsJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

        @Override
        public void writeTo(JsonGenerator generator, ILoggingEvent event) {
            Object[] arguments = event.getArgumentArray();
            if (arguments == null) {
                return;
            }
            for (int i = placeholders(event.getMessage()); i < arguments.length; i++) {
                if (arguments[i] instanceof StructuredArgument argument) {
                    argument.writeTo(generator);
                }
            }
        }
    }

    private static LoggingEventCompositeJsonFormatter createFormatter() {
        ContextBase context = new ContextBase();
        ContextAwareBase owner = new ContextAwareBase();
        owner.setContext(context);
        LoggingEventJsonProviders providers = new LoggingEventJsonProviders();
        providers.addLogstashMarkers(new LogstashMarkersJsonProvider());
        providers.addKeyValuePairs(new KeyValuePairsJsonProvider());
        providers.addProvider(new UnformattedStructuredArgumentsJsonProvider());
        LoggingEventCompositeJsonFormatter formatter = new LoggingEventCompositeJsonFormatter(owner);
        formatter.setContext(context);
        formatter.setProviders(providers);
        formatter.start();
        return formatter;
    }
}
