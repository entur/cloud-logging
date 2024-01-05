package no.entur.logging.cloud.grpc.trace;

import io.grpc.*;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor which copies tracing headers from the current Context.
 */

public class CopyCorrelationIdFromGrpcMdcContextToRequestClientInterceptor implements ClientInterceptor {

	private static final Logger log = LoggerFactory.getLogger(CopyCorrelationIdFromGrpcMdcContextToRequestClientInterceptor.class);

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
		GrpcMdcContext context = GrpcMdcContext.get();

		// does not make much sense to create this value if it is not already present
		// however it should always be present
		if (context != null) {
			String correlationId = context.get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY);
			if (correlationId != null) {
				return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

					@Override
					public void start(Listener<RespT> responseListener, Metadata metadata) {
						metadata.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
						super.start(responseListener, metadata);
					}
				};

			}
		}
		log.warn("No correlation-id available for {}", method.getFullMethodName());

		return next.newCall(method, callOptions);

	}
}
