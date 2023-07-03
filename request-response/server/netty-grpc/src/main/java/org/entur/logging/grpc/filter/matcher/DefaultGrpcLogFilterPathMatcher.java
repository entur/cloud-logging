package org.entur.logging.grpc.filter.matcher;

import org.entur.logging.grpc.filter.GrpcLogFilter;

public class DefaultGrpcLogFilterPathMatcher implements GrpcLogFilterPathMatcher {

	private final GrpcLogFilter filter;
	
	public DefaultGrpcLogFilterPathMatcher(GrpcLogFilter filter) {
		super();
		this.filter = filter;
	}

	@Override
	public boolean matches(String path) {
		return true;
	}

	@Override
	public GrpcLogFilter getFilter() {
		return filter;
	}

}
