package no.entur.logging.cloud.rr.grpc;

import io.grpc.*;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Interceptor which adds correlation id
 */

public class AddNewCorrelationIdToRequestClientInterceptor implements ClientInterceptor {

	private static final Logger log = LoggerFactory.getLogger(AddNewCorrelationIdToRequestClientInterceptor.class);

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
		String correlationId = UUID.randomUUID().toString();
		return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

			@Override
			public void start(Listener<RespT> responseListener, Metadata metadata) {
				metadata.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
				super.start(responseListener, metadata);
			}
		};
	}
}
