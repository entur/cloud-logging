package no.entur.logging.cloud.logback.logstash.test;

import java.util.function.Supplier;

import net.logstash.logback.marker.LogstashMarker;
import tools.jackson.core.JsonGenerator;

/**
 * Carries the body of an HTTP request or response for the AI files (AI mode), attached to the request-response marker. The
 * request-response marker leaves out bodies of some content types (for example <code>text/plain</code>) and truncates long ones;
 * this marker keeps the whole body. Writes nothing to JSON output, so logging assertions see the same fields as without AI mode.
 * The body can be given as a supplier, which is called once, when the body is first needed (when the event is written, so not for
 * events which on-demand logging discards).
 */
public class AiBodyMarker extends LogstashMarker {

    private static final long serialVersionUID = 1L;

    public static final String MARKER_NAME = "AI_BODY";

    private transient Supplier<String> supplier;
    private volatile String body;
    private final boolean json;

    public AiBodyMarker(String body, boolean json) {
        super(MARKER_NAME);
        this.body = body;
        this.json = json;
    }

    /** The supplier returns null (or throws) if the body cannot be read; the event is then written without it. */
    public AiBodyMarker(Supplier<String> body, boolean json) {
        super(MARKER_NAME);
        this.supplier = body;
        this.json = json;
    }

    public String getBody() {
        Supplier<String> pending = supplier;
        if (pending != null) {
            synchronized (this) {
                if (supplier != null) {
                    try {
                        body = supplier.get();
                    } catch (RuntimeException e) {
                        body = null;
                    }
                    supplier = null;
                }
            }
        }
        return body;
    }

    public boolean isJson() {
        return json;
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
