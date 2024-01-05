package no.entur.logging.cloud.grpc.trace;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Interceptor which copies tracing headers from the call options.
 */

public class CopyCorrelationIdFromCallOptionToRequestClientInterceptor implements ClientInterceptor {

	private static final Logger log = LoggerFactory.getLogger(CopyCorrelationIdFromCallOptionToRequestClientInterceptor.class);

	public static final String CORRELATION_ID_OPTION = "correlationId";
	public static final CallOptions.Key<String> CORRELATION_ID_OPTION_KEY = CallOptions.Key.of(CORRELATION_ID_OPTION, null);

	public static ValueBuilder newValueBuilder() {
		return new ValueBuilder();
	}

	public static class ValueBuilder {

		private String correlationId;

		public ValueBuilder withCorrelationId(String correlationId) {
			this.correlationId = correlationId;
			return this;
		}

		public ValueBuilder withRandomCorrelationId() {
			this.correlationId = UUID.randomUUID().toString();
			return this;
		}

		public ValueBuilder withSlf4jMdcContextCorrelationId() {
			String id = MDC.get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY);
			if(id == null) {
				throw new IllegalStateException("Expected correlation id in MDC context");
			}
			return withCorrelationId(id);
		}

		public ValueBuilder withGrpcMdcContextCorrelationId() {
			CorrelationIdGrpcMdcContext grpcMdcContext = CorrelationIdGrpcMdcContext.get();
			if(grpcMdcContext == null) {
				throw new IllegalStateException("Expected MDC context");
			}
			String id = grpcMdcContext.getCorrelationId();
			if(id == null) {
				throw new IllegalStateException("Expected correlation id grpc MDC context");
			}
			return withCorrelationId(id);
		}

		public ValueBuilder withMdcContextCorrelationId() {
			CorrelationIdGrpcMdcContext grpcMdcContext = CorrelationIdGrpcMdcContext.get();
			if(grpcMdcContext != null) {
				String id = grpcMdcContext.getCorrelationId();
				if (id != null) {
					return withCorrelationId(id);
				}
			}
			String id = MDC.get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY);
			if(id == null) {
				return withCorrelationId(id);
			}
			throw new IllegalStateException("Expected correlation id in either slf4j MDC context or grpc MDC context");
		}


		public String build() {
			if(correlationId == null) {
				throw new IllegalStateException("Expected correlation id");
			}
			return correlationId;
		}
	}	
	
	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
		String correlationId = callOptions.getOption(CORRELATION_ID_OPTION_KEY);

		if (correlationId != null) {
			return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

				@Override
				public void start(Listener<RespT> responseListener, Metadata metadata) {
					metadata.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
					super.start(responseListener, metadata);
				}
			};

		}
		log.warn("No correlation-id available for client call to {}", method.getFullMethodName());

		return next.newCall(method, callOptions);

	}
}
