package org.entur.logging.grpc.marker;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.marker.LogstashMarker;
import org.entur.logging.grpc.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class GrpcConnectionMarker extends LogstashMarker {

	private static final long serialVersionUID = 1L;

	protected final Map<String, ?> headers;
	protected final String remote;
	protected final String uri;
	protected final String type;
	protected final String origin;

	/**
	 * 
	 * Constructor
	 * 
	 * @param name marker name
	 * @param headers map with headers, or null
	 * @param remote remote address, or null
	 * @param uri request uri or path
	 * @param type type
	 * @param origin origin; local or remote
	 */	
	
	public GrpcConnectionMarker(String name, Map<String, ?> headers, String remote, String uri, String type, String origin) {
		super(name);
		this.headers = headers;
		this.remote = remote;
		this.uri = uri;
		this.type = type;
		this.origin = origin;
	}

	@Override
	public void writeTo(JsonGenerator generator) throws IOException {
		generator.writeFieldName("http");
		generator.writeStartObject();

		writeFields(generator);
		
		generator.writeEndObject();
	}
	
	protected void writeFields(JsonGenerator generator) throws IOException {
		generator.writeFieldName("uri");
		generator.writeString(uri);

		generator.writeFieldName("type");
		generator.writeString(type);

		if(remote != null) {
			generator.writeFieldName("remote");
			generator.writeString(remote);
		}
		
		generator.writeFieldName("origin");
		generator.writeString(origin);
		
		if(headers != null) {
			generator.writeFieldName("headers");
			generator.writeStartObject();
			
			for (Entry<String, ?> entry : headers.entrySet()) {
				generator.writeFieldName(entry.getKey());
				
				Object value = entry.getValue();
				if(value instanceof List) {
					generator.writeObject(value);
				} else {
					generator.writeStartArray();
					if(entry.getKey().equals("authorization")) {
						generator.writeObject(Utils.toSHA((String)value));
					} else {
						generator.writeObject(value);
					}
					generator.writeEndArray();
				}
			}
		
			generator.writeEndObject();
		}
		
	}
	
	
}
