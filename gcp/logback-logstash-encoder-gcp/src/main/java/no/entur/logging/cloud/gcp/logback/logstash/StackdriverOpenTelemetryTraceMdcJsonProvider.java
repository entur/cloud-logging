package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import tools.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;

import java.io.IOException;
import java.util.Map;

/**
 *
 * A simple MDC provider. Renames MDC field name trace_id and span_id to logging.googleapis.com/trace and logging.googleapis.com/spanId.
 *
 */

public class StackdriverOpenTelemetryTraceMdcJsonProvider extends AbstractJsonProvider<ILoggingEvent> {
    
    private final String projectId;

    public StackdriverOpenTelemetryTraceMdcJsonProvider() {
        this(resolveProjectId());
    }

    StackdriverOpenTelemetryTraceMdcJsonProvider(String projectId) {
        this.projectId = projectId;
    }

    private static String resolveProjectId() {
        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (projectId != null) {
            return projectId;
        }
        return System.getenv("GCP_PROJECT_ID");
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {
            
            String traceId = mdcProperties.get("trace_id");
            if(mdcProperties.get("logging.googleapis.com/trace") == null && traceId != null) {
                String traceValue = projectId !=null ? "projects/" + projectId + "/traces/" + traceId : traceId;
                generator.writeStringProperty("logging.googleapis.com/trace", traceValue);
            }

            String spanId = mdcProperties.get("span_id");
            if(mdcProperties.get("logging.googleapis.com/spanId") == null && spanId != null) {
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
