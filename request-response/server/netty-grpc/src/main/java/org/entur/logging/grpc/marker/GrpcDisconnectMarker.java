package org.entur.logging.grpc.marker;

import java.util.Map;

public class GrpcDisconnectMarker extends GrpcConnectionMarker {
	
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
	
	public GrpcDisconnectMarker(Map<String, ?> headers, String remote, String uri, String origin) {
		super(GrpcDisconnectMarker.class.getName(), headers, remote, uri, "disconnect", origin);
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
