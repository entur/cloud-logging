package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.marker.LogstashMarker;
import org.slf4j.Marker;
import tools.jackson.core.JsonGenerator;

import java.util.Iterator;
import java.util.List;

/**
 * Carries the compact variant of a log message (AI mode), so that the compact file and <code>&lt;file&gt;.full</code> can render the same
 * event differently: the event's message is the full variant, {@link CompactMessageConverter} prints this one instead.
 * Writes nothing to JSON output.
 */
public class CompactMessageMarker extends LogstashMarker {

    private static final long serialVersionUID = 1L;

    public static final String MARKER_NAME = "AI_COMPACT_MESSAGE";

    private final String compactMessage;

    /** True if information was cut (e.g. a truncated body), so the event should carry an id pointing to the full variant. */
    private final boolean truncated;

    public CompactMessageMarker(String compactMessage, boolean truncated) {
        super(MARKER_NAME);
        this.compactMessage = compactMessage;
        this.truncated = truncated;
    }

    public String getCompactMessage() {
        return compactMessage;
    }

    public boolean isTruncated() {
        return truncated;
    }

    @Override
    public void writeTo(JsonGenerator generator) {
        // intentionally empty: only used for the compact file
    }

    public static CompactMessageMarker find(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        if (markers == null) {
            return null;
        }
        for (Marker marker : markers) {
            CompactMessageMarker found = find(marker);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static CompactMessageMarker find(Marker marker) {
        if (marker instanceof CompactMessageMarker) {
            return (CompactMessageMarker) marker;
        }
        if (marker.hasReferences()) {
            Iterator<Marker> iterator = marker.iterator();
            while (iterator.hasNext()) {
                CompactMessageMarker found = find(iterator.next());
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // equality by name (as for other logstash markers)
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
