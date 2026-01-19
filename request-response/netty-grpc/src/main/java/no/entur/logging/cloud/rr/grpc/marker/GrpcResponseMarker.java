package no.entur.logging.cloud.rr.grpc.marker;

import tools.jackson.core.JsonGenerator;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;

import java.io.IOException;

public class GrpcResponseMarker extends GrpcConnectionMarker<GrpcResponse> {

	private static final long serialVersionUID = 1L;

	public GrpcResponseMarker(GrpcResponse message) {
		super(GrpcResponseMarker.class.getName(), message);
	}

	@Override
	protected void writeFields(JsonGenerator generator) {
		generator.writeName("status");
		generator.writeNumber(message.getStatusCode().value());

		super.writeFields(generator);

		String body = message.getBody();
		if (body != null) {
			writeBodyField(generator, body);
		}
	}

	protected void writeBodyField(JsonGenerator generator, String body) {
		generator.writeName("body");
		generator.writeRawValue(body);
	}

	// make spotbugs happy
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
