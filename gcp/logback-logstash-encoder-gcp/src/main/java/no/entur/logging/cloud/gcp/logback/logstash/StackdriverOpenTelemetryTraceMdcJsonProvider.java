package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import tools.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;

import java.io.IOException;
import java.util.Map;

/**
 *
 * A simple MDC provider. Renames MDC field name traceId to trace.
 *
 */

public class StackdriverOpenTelemetryTraceMdcJsonProvider extends AbstractJsonProvider<ILoggingEvent> {
    
    private final String projectId;

    public StackdriverOpenTelemetryTraceMdcJsonProvider() {
        this(System.getenv("GOOGLE_CLOUD_PROJECT"));
    }

    // package-private: lets a test supply a fixed project id without touching env vars
    StackdriverOpenTelemetryTraceMdcJsonProvider(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {
            String traceId = mdcProperties.get("trace_id");
            if(traceId != null && projectId != null) {
                generator.writeStringProperty("logging.googleapis.com/trace", "projects/" + projectId + "/traces/" + traceId);
            }
            String spanId = mdcProperties.get("span_id");
            if(spanId != null) {
                generator.writeStringProperty("logging.googleapis.com/spanId", spanId);
            }
            for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(key == null || value == null) {
                    continue;
                }
                generator.writeStringProperty(key, value);
            }
        }
    }

}
