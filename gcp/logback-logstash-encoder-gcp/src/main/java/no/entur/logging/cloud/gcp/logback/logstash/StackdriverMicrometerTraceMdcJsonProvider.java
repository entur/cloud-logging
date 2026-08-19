package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractJsonProvider;
import tools.jackson.core.JsonGenerator;

import java.util.Map;

/**
 * An MDC provider that maps Micrometer tracing MDC keys to the special JSON fields
 * recognized by Google Cloud Logging.
 *
 * <p>When the Google Cloud Logging agent ingests structured JSON written to stdout, it promotes
 * recognized JSON fields into the corresponding {@code LogEntry} fields. In particular:
 * <ul>
 *     <li>{@code logging.googleapis.com/trace} becomes {@code LogEntry.trace}</li>
 *     <li>{@code logging.googleapis.com/spanId} becomes {@code LogEntry.spanId}</li>
 * </ul>
 * Unrecognized fields remain in {@code LogEntry.jsonPayload}.
 *
 * @see <a href="https://docs.cloud.google.com/logging/docs/agent/logging/configuration#special-fields">
 *     Special fields in structured payloads
 * </a>
 */
public class StackdriverMicrometerTraceMdcJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    public static final String MICROMETER_TRACE_ID_KEY = "traceId";
    public static final String MICROMETER_SPAN_ID_KEY = "spanId";
    public static final String MICROMETER_SAMPLED_KEY = "traceSampled";

    public static final String GCP_TRACE_KEY = "logging.googleapis.com/trace";
    public static final String GCP_SPAN_ID_KEY = "logging.googleapis.com/spanId";
    public static final String GCP_TRACE_SAMPLED = "logging.googleapis.com/trace_sampled";

    protected final String projectId;

    public StackdriverMicrometerTraceMdcJsonProvider(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties == null || mdcProperties.isEmpty()) {
            return;
        }

        // map micrometer MDC keys to GCP special fields; write all others as-is.
        for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            String value = entry.getValue();
            if (value == null) continue;

            switch (key) {
                case MICROMETER_TRACE_ID_KEY -> generator.writeStringProperty(GCP_TRACE_KEY, projectId != null ? "projects/" + projectId + "/traces/" + value : value);
                case MICROMETER_SPAN_ID_KEY  -> generator.writeStringProperty(GCP_SPAN_ID_KEY, value);
                case MICROMETER_SAMPLED_KEY -> {
                    generator.writeBooleanProperty(GCP_TRACE_SAMPLED, Boolean.parseBoolean(value));
                }
                default -> generator.writeStringProperty(key, value);
            }
        }
    }

}
