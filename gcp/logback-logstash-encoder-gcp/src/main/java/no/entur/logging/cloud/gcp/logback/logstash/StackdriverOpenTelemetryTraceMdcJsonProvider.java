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

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {
            String traceId = mdcProperties.get("traceId");
            if(traceId != null) {
                generator.writeStringProperty("trace", traceId);
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
