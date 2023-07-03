package org.entur.logging.grpc.filter;

import org.entur.logging.grpc.filter.matcher.DefaultGrpcLogFilterPathMatcher;
import org.entur.logging.grpc.filter.matcher.GrpcLogFilterPathMatcher;
import org.entur.logging.grpc.filter.matcher.PrefixGrpcLogFilterPathMatcher;

import java.util.ArrayList;
import java.util.List;

public class GrpcServerLoggingFilters {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static GrpcServerLoggingFilters full() {
		return new GrpcServerLoggingFilters.Builder().fullDefaultLogging().build();
	}

	public static GrpcServerLoggingFilters classic() {
		return new GrpcServerLoggingFilters.Builder().classicDefaultLogging().build();
	}

	public static class Builder {

		private List<GrpcLogFilterPathMatcher> requests = new ArrayList<>();

		private GrpcLogFilterPathMatcher last;

		public Builder filter(GrpcLogFilterPathMatcher matcher) {
			requests.add(matcher);
			return this;
		}

		public Builder defaultFilter(GrpcLogFilter matcher) {
			last = new DefaultGrpcLogFilterPathMatcher(matcher);
			return this;
		}

		public Builder fullLoggingForPrefix(String path) {
			requests.add(new PrefixGrpcLogFilterPathMatcher(path, GrpcLogFilter.FULL));
			return this;
		}

		public Builder classicLoggingForPrefix(String path) {
			requests.add(new PrefixGrpcLogFilterPathMatcher(path, GrpcLogFilter.REQUEST_RESPONSE));
			return this;
		}

		public Builder summaryLoggingForPrefix(String path) {
			requests.add(new PrefixGrpcLogFilterPathMatcher(path, GrpcLogFilter.SUMMARY));
			return this;
		}

		public Builder noLoggingForPrefix(String path) {
			requests.add(new PrefixGrpcLogFilterPathMatcher(path, GrpcLogFilter.NONE));
			return this;
		}

		public Builder noDefaultLogging() {
			return defaultFilter(GrpcLogFilter.NONE);
		}

		public Builder summaryDefaultLogging() {
			return defaultFilter(GrpcLogFilter.SUMMARY);
		}

		public Builder classicDefaultLogging() {
			return defaultFilter(GrpcLogFilter.REQUEST_RESPONSE);
		}

		public Builder fullDefaultLogging() {
			return defaultFilter(GrpcLogFilter.FULL);
		}

		public GrpcServerLoggingFilters build() {
			if(last == null) {
				throw new IllegalStateException("Expected default behaviour");
			}

			// add catch all last
			requests.add(last);

			return new GrpcServerLoggingFilters(requests);
		}
	}

	protected final GrpcLogFilterPathMatcher[] requests;

	public GrpcServerLoggingFilters(List<GrpcLogFilterPathMatcher> requests) {
		this(requests.toArray(new GrpcLogFilterPathMatcher[requests.size()]));
	}

	public GrpcServerLoggingFilters(GrpcLogFilterPathMatcher[] requests) {
		this.requests = requests;
	}

	public GrpcLogFilter getFilter(String path) {
		for(GrpcLogFilterPathMatcher matcher : requests) {
			if(matcher.matches(path)) {
				return matcher.getFilter();
			}
		}
		return null;
	}

	protected GrpcLogFilterPathMatcher[] getRequests() {
		return requests;
	}

}
