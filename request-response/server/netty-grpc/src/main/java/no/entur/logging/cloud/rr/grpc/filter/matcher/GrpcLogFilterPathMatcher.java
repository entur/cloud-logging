package no.entur.logging.cloud.rr.grpc.filter.matcher;

import no.entur.logging.cloud.rr.grpc.filter.GrpcLogFilter;

public interface GrpcLogFilterPathMatcher {

	public boolean matches(String path);
	
	public GrpcLogFilter getFilter();
}
