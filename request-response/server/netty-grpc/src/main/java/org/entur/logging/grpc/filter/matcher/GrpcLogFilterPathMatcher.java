package org.entur.logging.grpc.filter.matcher;

import org.entur.logging.grpc.filter.GrpcLogFilter;

public interface GrpcLogFilterPathMatcher {

	public boolean matches(String path);
	
	public GrpcLogFilter getFilter();
}
