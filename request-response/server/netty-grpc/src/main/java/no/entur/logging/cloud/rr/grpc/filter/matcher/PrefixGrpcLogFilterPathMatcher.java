package no.entur.logging.cloud.rr.grpc.filter.matcher;

import no.entur.logging.cloud.rr.grpc.filter.GrpcLogFilter;

public class PrefixGrpcLogFilterPathMatcher implements GrpcLogFilterPathMatcher {

	private final String prefix;
	private final GrpcLogFilter filter;
	
	public PrefixGrpcLogFilterPathMatcher(String prefix, GrpcLogFilter filter) {
		this.prefix = prefix;
		this.filter = filter;
	}

	@Override
	public boolean matches(String path) {
		return path.startsWith(prefix);
	}

	@Override
	public GrpcLogFilter getFilter() {
		return filter;
	}

}
