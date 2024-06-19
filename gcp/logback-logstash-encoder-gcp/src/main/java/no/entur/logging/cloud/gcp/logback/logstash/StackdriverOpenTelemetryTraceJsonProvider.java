package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;

import java.io.IOException;
import java.util.Map;

/**
 *
 * Add fields so to rename the traceId MDC field to trace for use with GCP Trace explorer.<br>
 * <br>
 * See https://cloud.google.com/logging/docs/structured-logging for more info.
 * <br><br>
 * Note: Exclude traceId and spanId in MDC provider.
 *
 */

public class StackdriverOpenTelemetryTraceJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent iLoggingEvent) throws IOException {
        Map<String, String> mdcPropertyMap = iLoggingEvent.getMDCPropertyMap();

        // copy value to another field name
        String trace = mdcPropertyMap.get("traceId");
        if(trace != null) {
            generator.writeStringField("trace", trace);
        }

        // copy through
        String spanId = mdcPropertyMap.get("spanId");
        if(spanId != null) {
            generator.writeStringField("spanId", spanId);
        }
    }
}
