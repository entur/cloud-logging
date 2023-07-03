package org.entur.logging.grpc.marker;

import java.util.Map;

public class GrpcConnectMarker extends GrpcConnectionMarker {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 * Constructor
	 * 
	 * @param headers map with headers, or null
	 * @param remote remote address, or null
	 * @param uri request uri or path
	 * @param origin origin; local or remote
	 */
	
	public GrpcConnectMarker(Map<String, ?> headers, String remote, String uri, String origin) {
		super(GrpcConnectMarker.class.getName(), headers, remote, uri, "connect", origin);
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
