package org.entur.logging.grpc.marker;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Map;

public class GrpcResponseMarker extends GrpcPayloadMarker {

	private static final long serialVersionUID = 1L;

	private final int status;

	/**
	 * 
	 * Constructor
	 * 
	 * @param headers map with headers, or null
	 * @param body body or null 
	 * @param remote remote address, or null
	 * @param uri request uri or path
	 * @param origin remote (i.e. for incoming) or local (i.e. for outgoing)
	 * @param status HTTP status
	 */	
	
	public GrpcResponseMarker(Map<String, ?> headers, String remote, String uri, String body, String origin, int status) {
		super(GrpcResponseMarker.class.getName(), headers, remote, uri, "response", body, origin);
		this.status = status;
	}

	@Override
	protected void writeFields(JsonGenerator generator) throws IOException {

		generator.writeFieldName("status");
		generator.writeNumber(status);
		
		super.writeFields(generator);

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
