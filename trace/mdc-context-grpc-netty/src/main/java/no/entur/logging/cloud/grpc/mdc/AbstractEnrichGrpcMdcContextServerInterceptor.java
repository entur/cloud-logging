package no.entur.logging.cloud.grpc.mdc;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor for mapping requests (and/or other input sources) to MDC fields.
 */

public abstract class AbstractEnrichGrpcMdcContextServerInterceptor implements ServerInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEnrichGrpcMdcContextServerInterceptor.class);

	protected final EnrichGrpcMdcContextServerInterceptor enricher;

	public AbstractEnrichGrpcMdcContextServerInterceptor(EnrichGrpcMdcContextServerInterceptor enricher) {
		this.enricher = enricher;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
		if (grpcMdcContext != null) {
			// omit cleanup, fields will die as part of the mdc context
			enricher.enrich(call, headers, grpcMdcContext);

			return next.startCall(call, headers);
		} else {
			// ignore
			LOGGER.warn("Unable to update gRPC MDC context, please add " + InitializeGrpcMdcContextServerInterceptor.class.getName() + " before this interceptor");
			return next.startCall(call, headers);
		}
	}

}
