package no.entur.logging.cloud.rr.grpc.marker;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.marker.LogstashMarker;
import no.entur.logging.cloud.rr.grpc.message.GrpcMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class GrpcConnectionMarker<T extends GrpcMessage> extends LogstashMarker {

	private static final long serialVersionUID = 1L;

	protected final T message;

	protected GrpcConnectionMarker(String name, T message) {
		super(name);
		this.message = message;
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
		generator.writeString(message.getUri());

		generator.writeFieldName("type");
		generator.writeString(message.getType());

		String remote = message.getRemote();
		if(remote != null) {
			generator.writeFieldName("remote");
			generator.writeString(remote);
		}

		String origin = message.getOrigin();
		generator.writeFieldName("origin");
		generator.writeString(origin);

		Map<String, ?> headers = message.getHeaders();
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
					generator.writeObject(value);
					generator.writeEndArray();
				}
			}
		
			generator.writeEndObject();
		}
		
	}
	
	
}
