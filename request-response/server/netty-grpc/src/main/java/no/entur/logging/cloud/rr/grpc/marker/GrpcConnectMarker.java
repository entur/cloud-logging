package no.entur.logging.cloud.rr.grpc.marker;

import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;

public class GrpcConnectMarker extends GrpcConnectionMarker<GrpcConnect> {
	
	private static final long serialVersionUID = 1L;

	public GrpcConnectMarker(GrpcConnect message) {
		super(GrpcConnectMarker.class.getName(), message);
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
