package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import tools.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;

import java.util.Map;

/**
 *
 * A simple MDC provider. Renames MDC field name trace_id and span_id to logging.googleapis.com/trace and logging.googleapis.com/spanId.
 * Falls back to traceId/spanId (Micrometer Tracing's MDC convention) when the OpenTelemetry Java agent's
 * snake_case keys are not present, since consumers of this library are not required to use the agent.
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
        if (isUsable(projectId)) {
            return projectId;
        }
        projectId = System.getenv("GCP_PROJECT_ID");
        if (isUsable(projectId)) {
            return projectId;
        }
        return null;
    }

    private static boolean isUsable(String value) {
        return value != null && !value.isBlank();
    }

    // W3C trace-flags is a 2-character hex byte; bit 0 is the "sampled" flag.
    private static Boolean parseSampled(String traceFlags) {
        try {
            return (Integer.parseInt(traceFlags, 16) & 0x1) != 0;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {

            String traceId = mdcProperties.get("trace_id");
            if(traceId == null) {
                // Micrometer Tracing's MDC convention as fallback for users not using the OpenTelemetry Java agent
                traceId = mdcProperties.get("traceId");
            }
            if(mdcProperties.get("logging.googleapis.com/trace") == null && traceId != null) {
                String traceValue = projectId != null ? "projects/" + projectId + "/traces/" + traceId : traceId;
                generator.writeStringProperty("logging.googleapis.com/trace", traceValue);
            }

            String spanId = mdcProperties.get("span_id");
            if(spanId == null) {
                // Micrometer Tracing's MDC convention as fallback for users not using the OpenTelemetry Java agent
                spanId = mdcProperties.get("spanId");
            }
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
