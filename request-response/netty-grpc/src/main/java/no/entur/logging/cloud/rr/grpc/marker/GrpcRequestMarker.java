package no.entur.logging.cloud.rr.grpc.marker;

import com.fasterxml.jackson.core.JsonGenerator;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;

import java.io.IOException;

public class GrpcRequestMarker extends GrpcConnectionMarker<GrpcRequest> {

	private static final long serialVersionUID = 1L;

	public GrpcRequestMarker(GrpcRequest message) {
		super(GrpcRequestMarker.class.getName(), message);
	}

	@Override
	protected void writeFields(JsonGenerator generator) throws IOException {
		super.writeFields(generator);

		long timeRemaining = message.getTimeRemainingUntilDeadlineInMilliseconds();
		if(timeRemaining != -1L) {
			generator.writeFieldName("deadline-in");
			generator.writeNumber(timeRemaining);
		}

		String body = message.getBody();
		if(body != null) {
			writeBodyField(generator, body);
		}
	}

	protected void writeBodyField(JsonGenerator generator, String body) throws IOException {
		generator.writeFieldName("body");
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
