package no.entur.logging.cloud.rr.grpc.marker;

import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;

public class GrpcDisconnectMarker extends GrpcConnectionMarker<GrpcDisconnect> {
	
	private static final long serialVersionUID = 1L;
	
	public GrpcDisconnectMarker(GrpcDisconnect message) {
		super(GrpcDisconnectMarker.class.getName(), message);
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
