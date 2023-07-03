package org.entur.logging.grpc.trace;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import org.entur.logging.grpc.mdc.CustomizableMdcContextInterceptor;
import org.entur.logging.grpc.mdc.GrpcMdcContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class GrpcTraceMdcContextInterceptor extends CustomizableMdcContextInterceptor {

	// utility method for regular HTTP clients etc
	public static Map<String, String> getDownstreamHeaderFromMdcContext() {
		GrpcMdcContext context = GrpcMdcContext.get();

		String correlationId = null;
		if (context != null) {
			correlationId = context.get(GrpcTraceMdcContext.CORRELATION_ID_MDC_KEY);
		}

		Map<String, String> result = new HashMap<>();
		if (correlationId != null) {
			result.put(GrpcTraceMdcContext.CORRELATION_ID_HEADER, correlationId);
		}
		return result;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private BiFunction<ServerCall, Metadata, Map<String, String>> mdcMapper;

		public GrpcTraceMdcContextInterceptor build() {
			if (mdcMapper == null) {
				mdcMapper = (s, m) -> {
					GrpcTraceMdcContext traceContext = GrpcTraceMdcContext.getContext(m);

					return traceContext.getContext();
				};
			}

			return new GrpcTraceMdcContextInterceptor(mdcMapper);
		}

		public Builder withMdcMapper(BiFunction<ServerCall, Metadata, Map<String, String>> mdcMapper) {
			this.mdcMapper = mdcMapper;

			return this;
		}
	}

	protected GrpcTraceMdcContextInterceptor(BiFunction<ServerCall, Metadata, Map<String, String>> mdcMapper) {
		super(mdcMapper);
	}

}
