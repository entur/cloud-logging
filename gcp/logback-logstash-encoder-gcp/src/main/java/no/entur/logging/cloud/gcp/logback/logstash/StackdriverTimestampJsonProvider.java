package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import tools.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;

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
	public void writeTo(JsonGenerator generator, ILoggingEvent event) {
		generator.writeObjectPropertyStart("timestamp");
		generator.writeNumberProperty("seconds", event.getTimeStamp() / 1000);
		generator.writeNumberProperty("nanos", event.getNanoseconds());
		generator.writeEndObject();
	}

}