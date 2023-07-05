package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import net.logstash.logback.marker.RawJsonAppendingMarker;

import java.io.IOException;

/**
 * Auto-detecting pretty-printing marker. Pretty-printing cannot be performed in advance.
 *
 */

public class PrettyPrintingMarker extends RawJsonAppendingMarker {
	
	private static final long serialVersionUID = 1L;

	public PrettyPrintingMarker(String fieldName, String rawJson) {
		super(fieldName, rawJson);
	}

	@Override
	protected void writeFieldValue(JsonGenerator generator) throws IOException {
		generator.flush();
		
		PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
		if(prettyPrinter == null) {
			super.writeFieldValue(generator);
		} else {
			// append to existing tree event by event
	        final JsonParser parser = generator.getCodec().getFactory().createParser((String)super.getFieldValue());

	        try {
	            while (parser.nextToken() != null) {
	                generator.copyCurrentEvent(parser);
	            }
	        } finally {
	            parser.close();
	        }
		}
	}


}
