package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.spi.ContextAwareBase;
import net.logstash.logback.composite.loggingevent.KeyValuePairsJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventCompositeJsonFormatter;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LogstashMarkersJsonProvider;
import net.logstash.logback.marker.LogstashMarker;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * Prints the structured fields of an event which are not part of its message, <code> | fields: {"key":value, ...}</code>: the fields
 * of Logstash markers (for example <code>Markers.append("system", map)</code>) and SLF4J key-value pairs, as in the JSON output.
 * Empty if there are none, and for request-response events (their message already has the details). UUIDs are aliased.
 * With option <code>compact</code> (for the compact file), the JSON is truncated like request-response bodies
 * (see {@link AiLoggingControl#AI_BODY_MAX_PROPERTY}).
 */
public class StructuredFieldsConverter extends ClassicConverter {

    public static final String PREFIX = " | fields: ";

    private static final LoggingEventCompositeJsonFormatter FORMATTER = createFormatter();

    private boolean compact;

    @Override
    public void start() {
        compact = "compact".equals(getFirstOption());
        super.start();
    }

    @Override
    public String convert(ILoggingEvent event) {
        String json = toJson(event);
        if (json == null) {
            return "";
        }
        if (compact) {
            json = AiLoggingControl.truncateBody(json);
        }
        return PREFIX + MdcAliasConverter.aliasUuids(json);
    }

    /** True if the compact file cuts the structured fields of the event. */
    public static boolean isTruncatedInCompactFile(ILoggingEvent event) {
        String json = toJson(event);
        return json != null && !AiLoggingControl.truncateBody(json).equals(json);
    }

    /** The structured fields as a single-line JSON object, or null if there are none. */
    public static String toJson(ILoggingEvent event) {
        if (!hasStructuredFields(event)) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(256);
        try {
            FORMATTER.writeEvent(event, out);
        } catch (IOException | RuntimeException e) {
            return null;
        }
        String json = CompactMessageConverter.singleLine(out.toString(StandardCharsets.UTF_8).strip());
        return json.isEmpty() || json.equals("{}") ? null : json;
    }

    private static boolean hasStructuredFields(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
        boolean hasKeyValuePairs = keyValuePairs != null && !keyValuePairs.isEmpty();
        if (markers == null || markers.isEmpty()) {
            return hasKeyValuePairs;
        }
        if (CompactMessageMarker.find(event) != null) {
            // request-response logging: the message has headers and body
            return false;
        }
        if (hasKeyValuePairs) {
            return true;
        }
        for (Marker marker : markers) {
            if (hasLogstashMarker(marker)) {
                return true;
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

    private static LoggingEventCompositeJsonFormatter createFormatter() {
        ContextBase context = new ContextBase();
        ContextAwareBase owner = new ContextAwareBase();
        owner.setContext(context);
        LoggingEventJsonProviders providers = new LoggingEventJsonProviders();
        providers.addLogstashMarkers(new LogstashMarkersJsonProvider());
        providers.addKeyValuePairs(new KeyValuePairsJsonProvider());
        LoggingEventCompositeJsonFormatter formatter = new LoggingEventCompositeJsonFormatter(owner);
        formatter.setContext(context);
        formatter.setProviders(providers);
        formatter.start();
        return formatter;
    }
}
