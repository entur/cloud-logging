package no.entur.logging.cloud.rr.grpc.marker;

import tools.jackson.core.JsonGenerator;
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
	public void writeTo(JsonGenerator generator) {
		generator.writeName("http");
		generator.writeStartObject();

		writeFields(generator);
		
		generator.writeEndObject();
	}
	
	protected void writeFields(JsonGenerator generator) {

        String uri = message.getUri();
        if(uri != null) {
            generator.writeName("uri");
            generator.writeString(uri);
        }

        String type = message.getType();
        if(type != null) {
            generator.writeName("type");
            generator.writeString(type);
        }

		String remote = message.getRemote();
		if(remote != null) {
			generator.writeName("remote");
			generator.writeString(remote);
		}

		String origin = message.getOrigin();
        if(origin != null) {
            generator.writeName("origin");
            generator.writeString(origin);
        }

		Map<String, ?> headers = message.getHeaders();
		generator.writeName("headers");
		generator.writeStartObject();

		if(headers != null) {
			for (Entry<String, ?> entry : headers.entrySet()) {

				String key = entry.getKey();
				if(key != null && !key.isEmpty()) {
					generator.writeName(key.toLowerCase());
					generator.writeStartArray();

					Object value = entry.getValue();
					if(value != null) {
						if (value instanceof List) {
							List<Object> values = (List) value;
							for (Object listValue : values) {
								generator.writePOJO(listValue);
							}
						} else {
							generator.writePOJO(value);
						}
					}
					generator.writeEndArray();
				}
			}
		
		}
		generator.writeEndObject();

	}
	
	
}
