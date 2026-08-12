package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractJsonProvider;
import tools.jackson.core.JsonGenerator;

import java.util.Map;

/**
 * An MDC provider that maps OpenTelemetry trace fields to the special JSON fields
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

    public static final String GCP_TRACE_KEY = "logging.googleapis.com/trace";
    public static final String GCP_SPAN_ID_KEY = "logging.googleapis.com/spanId";

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(key == null || value == null) {
                    continue;
                }

                if (MICROMETER_TRACE_ID_KEY.equals(key)) {
                    generator.writeStringProperty(GCP_TRACE_KEY, value);
                } else if(MICROMETER_SPAN_ID_KEY.equals(key)) {
                    generator.writeStringProperty(GCP_SPAN_ID_KEY, value);
                } else {
                    generator.writeStringProperty(key, value);
                }
            }
        }
    }

}
