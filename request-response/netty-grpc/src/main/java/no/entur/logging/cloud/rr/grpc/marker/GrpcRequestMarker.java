package no.entur.logging.cloud.rr.grpc.marker;

import no.entur.logging.cloud.appender.scope.LoggingScopePostProcessing;
import tools.jackson.core.JsonGenerator;
import no.entur.logging.cloud.rr.grpc.message.GrpcPayload;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;

import java.io.IOException;

public class GrpcRequestMarker extends GrpcConnectionMarker<GrpcRequest> implements LoggingScopePostProcessing {

	private static final long serialVersionUID = 1L;

	public GrpcRequestMarker(GrpcRequest message) {
		super(GrpcRequestMarker.class.getName(), message);
	}

	@Override
	public void performPostProcessing() {
		// the log statement is going to be written; map the body before it is handed to the async logging thread,
		// unless the payload is configured to be mapped on the async logging thread
		GrpcPayload payload = message.getPayload();
		if(payload != null) {
			payload.prepareForAsyncAppender();
		}
	}

	@Override
	protected void writeFields(JsonGenerator generator) {
		super.writeFields(generator);

		long timeRemaining = message.getTimeRemainingUntilDeadlineInMilliseconds();
		if(timeRemaining != -1L) {
			generator.writeName("deadline-in");
			generator.writeNumber(timeRemaining);
		}

		String body = message.getBody();
		if(body != null) {
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
