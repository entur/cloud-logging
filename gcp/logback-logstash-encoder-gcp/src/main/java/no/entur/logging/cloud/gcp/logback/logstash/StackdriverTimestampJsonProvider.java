package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;

import java.io.IOException;

/**
 * Add machine-readable timestamp.
 *
 * According to https://cloud.google.com/logging/docs/agent/logging/configuration#timestamp-processing:
 *
 * If the JSON timestamp representations is present in a structured record,
 * the Logging agent removes the fields from jsonPayload and collapses them into a single representation in the timestamp field in the LogEntry object.
 *
 */

public class StackdriverTimestampJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        generator.writeObjectFieldStart("timestamp");

        generator.writeNumberField("seconds", event.getTimeStamp() / 1000);

        int nanoseconds = event.getNanoseconds(); // May return -1 if data unavailable.
        if(nanoseconds >= 0) {
            generator.writeNumberField("nanos", nanoseconds % 1000_000_000);
        } else {
            // get nanos from milliseconds
            generator.writeNumberField("nanos", (event.getTimeStamp() % 1000) * 1000_000);
        }
        generator.writeEndObject();
    }

}