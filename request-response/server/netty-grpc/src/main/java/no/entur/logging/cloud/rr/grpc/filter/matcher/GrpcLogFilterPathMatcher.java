package no.entur.logging.cloud.rr.grpc.filter.matcher;

import no.entur.logging.cloud.rr.grpc.filter.GrpcLogFilter;

public interface GrpcLogFilterPathMatcher {

	boolean matches(String path);
	
	GrpcLogFilter getFilter();
}
