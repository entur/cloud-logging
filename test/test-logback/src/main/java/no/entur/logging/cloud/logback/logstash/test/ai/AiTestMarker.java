package no.entur.logging.cloud.logback.logstash.test.ai;

import java.util.List;

import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.marker.LogstashMarker;
import tools.jackson.core.JsonGenerator;

/**
 * Marks an event as the start or the end of a test (or test class) in AI mode. The event is queued like any other event, so it is in
 * order with the events logged during the test, and {@link AiFileAppender} writes its line as it is:
 * <pre>
 * ---- #t17 start: DocumentEndpointTest.returnsNotFound() ----
 * ---- #t17 end (FAILED): DocumentEndpointTest.returnsNotFound() ----
 * </pre>
 * so that <code>sed -n '/#t17 start/,/#t17 end/p'</code> prints what was logged during the test. Written by
 * {@code AiTestExecutionListener}, which lists the failed tests and their tokens in the summary file.
 */
public class AiTestMarker extends LogstashMarker {

    private static final long serialVersionUID = 1L;

    public static final String MARKER_NAME = "AI_TEST";

    private final String token;
    private final boolean start;
    private final boolean failed;
    private final String line;

    public static AiTestMarker start(String token, String name) {
        return new AiTestMarker(token, true, false, "---- #" + token + " start: " + name + " ----");
    }

    /** @param outcome for example <code>passed</code>, <code>FAILED</code> or <code>aborted</code> */
    public static AiTestMarker end(String token, String name, String outcome, boolean failed) {
        return new AiTestMarker(token, false, failed, "---- #" + token + " end (" + outcome + "): " + name + " ----");
    }

    private AiTestMarker(String token, boolean start, boolean failed, String line) {
        super(MARKER_NAME);
        this.token = token;
        this.start = start;
        this.failed = failed;
        this.line = line;
    }

    /** The test marker of the event, or null. */
    public static AiTestMarker of(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        if (markers != null) {
            for (int i = 0; i < markers.size(); i++) {
                if (markers.get(i) instanceof AiTestMarker marker) {
                    return marker;
                }
            }
        }
        return null;
    }

    /** Unique within the files of a test JVM: a counter (<code>t17</code>). */
    public String getToken() {
        return token;
    }

    public boolean isStart() {
        return start;
    }

    /** For an end marker: whether the test failed. Such markers are always written. */
    public boolean isFailed() {
        return failed;
    }

    /** The line to write, without line separator. */
    public String getLine() {
        return line;
    }

    @Override
    public void writeTo(JsonGenerator generator) {
        // intentionally empty: only used for the AI files
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
