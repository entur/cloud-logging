package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import tools.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;

import java.lang.management.ManagementFactory;
import java.util.List;
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
public class StackdriverOpenTelemetryTraceMdcJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    public static final String OPENTELEMETRY_TRACE_ID_KEY = "trace_id";
    public static final String OPENTELEMETRY_SPAN_ID_KEY = "span_id";
    public static final String OPENTELEMETRY_TRACE_FLAGS_KEY = "trace_flags";

    public static final String GCP_TRACE_KEY = "logging.googleapis.com/trace";
    public static final String GCP_SPAN_ID_KEY = "logging.googleapis.com/spanId";
    public static final String GCP_TRACE_SAMPLED = "logging.googleapis.com/trace_sampled";

    protected final String tracePrefix;

    public StackdriverOpenTelemetryTraceMdcJsonProvider(String projectId) {
        this.tracePrefix = projectId != null ? "projects/" + projectId + "/traces/" : null;
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties == null || mdcProperties.isEmpty()) {
            return;
        }

        // map OTel MDC keys to GCP special fields; write all others as-is.
        for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            String value = entry.getValue();
            if (value == null) continue;

            switch (key) {
                case OPENTELEMETRY_TRACE_ID_KEY -> generator.writeStringProperty(GCP_TRACE_KEY, tracePrefix != null ? tracePrefix + value : value);
                case OPENTELEMETRY_SPAN_ID_KEY  -> generator.writeStringProperty(GCP_SPAN_ID_KEY, value);
                case OPENTELEMETRY_TRACE_FLAGS_KEY -> {
                    if (isSampled(value)) {
                        generator.writeBooleanProperty(GCP_TRACE_SAMPLED, true);
                    }
                }
                default -> generator.writeStringProperty(key, value);
            }
        }
    }

    // W3C trace-flags is a 2-character hex byte; bit 0 is the "sampled" flag.
    private static boolean isSampled(String traceFlags) {
        if (traceFlags.length() != 2) return false;
        char last = traceFlags.charAt(1);
        // '1' (0x01) and '3' (0x03) have bit 0 set
        return last == '1' || last == '3';
    }

    public static boolean isOtelAgent() {
        // 1. Check direct JVM command-line arguments (-javaagent)
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : jvmArgs) {
            if (isOtelArgument(arg)) {
                return true;
            }
        }

        // 2. Backup check for environment variables that inject JVM arguments
        String javaToolOptions = System.getenv("JAVA_TOOL_OPTIONS");
        if (javaToolOptions != null && isOtelArgument(javaToolOptions)) {
            return true;
        }

        return false;
    }

    private static boolean isOtelArgument(String argument) {
        String lowerArg = argument.toLowerCase();
        return lowerArg.contains("-javaagent:") && lowerArg.contains("opentelemetry");
    }

}
