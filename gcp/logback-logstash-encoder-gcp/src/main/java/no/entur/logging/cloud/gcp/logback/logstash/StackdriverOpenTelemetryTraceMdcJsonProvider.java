package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import java.io.IOException;
import java.util.Map;

/**
 *
 * A simpler MDC provider. Renames MDC field name traceId to trace
 *
 */

public class StackdriverOpenTelemetryTraceMdcJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {
            if(mdcProperties.containsKey("traceId")) {
                for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if(key == null || value == null) {
                        continue;
                    }

                    if(key.equals("traceId")) {
                        generator.writeStringField("trace", value);
                        continue;
                    }

                    generator.writeStringField(key, value);
                }
            } else {
                for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if(key == null || value == null) {
                        continue;
                    }
                    generator.writeStringField(key, value);
                }
            }
        }
    }

}
