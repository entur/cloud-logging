package org.entur.logging.grpc.marker;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Map;

public class GrpcRequestMarker extends GrpcPayloadMarker {

	private static final long serialVersionUID = 1L;

	private final String method;

	/**
	 * 
	 * Constructor
	 * 
	 * @param headers map with headers, or null
	 * @param body body or null 
	 * @param remote remote address, or null
	 * @param uri request uri or path
	 * @param method http method
	 * @param origin remote (i.e. for incoming) or local (i.e. for outgoing)
	 */
	
	public GrpcRequestMarker(Map<String, ?> headers, String remote, String uri, String body, String method, String origin) {
		super(GrpcRequestMarker.class.getName(), headers, remote, uri, "request", body, origin);
		this.method = method;
	}

	@Override
	protected void writeFields(JsonGenerator generator) throws IOException {
		generator.writeFieldName("method");
		generator.writeString(method);
		
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

	public String getMethod() {
		return method;
	}

}
