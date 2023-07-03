package org.entur.logging.grpc.marker;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;

import java.io.IOException;
import java.util.Map;

/**
 * Auto-detecting pretty-printing marker. Pretty-printing cannot be performed in advance.
 */

public abstract class GrpcMarker extends GrpcConnectionMarker {

	private static final long serialVersionUID = 1L;

	protected final String body;

	/**
	 * 
	 * Constructor
	 * 
	 * @param name marker name
	 * @param headers map with headers, or null
	 * @param remote remote address, or null
	 * @param uri request uri or path
	 * @param type type
	 * @param body body or null 
	 * @param origin origin; local or remote
	 */		

	public GrpcMarker(String name, Map<String, String> headers, String remote, String uri, String type, String body, String origin) {
		super(name, headers, remote, uri, type, origin);
		this.body = body;
	}

	protected void writeFields(JsonGenerator generator) throws IOException {
		super.writeFields(generator);
		
		if(body != null) {
			generator.writeFieldName("body");

			PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
			if(prettyPrinter == null) {
				generator.writeRawValue(body);
			} else {
				// append to existing tree event by event
				final JsonParser parser = generator.getCodec().getFactory().createParser(body);

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

	@Override
	public String toString() { // called by non-json loggers
		return '\n' + body;
	}
}
