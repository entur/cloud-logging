package no.entur.logging.cloud.grpc.trace;

import io.grpc.*;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Interceptor which copies tracing headers from the current Context.
 */

public class FixedCorrelationIdClientInterceptor implements ClientInterceptor {

	private static final Logger log = LoggerFactory.getLogger(FixedCorrelationIdClientInterceptor.class);

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private String correlationId;

		public Builder withCorrelationId(String correlationId) {
			this.correlationId = correlationId;
			return this;
		}

		public Builder withRandomCorrelationId() {
			this.correlationId = UUID.randomUUID().toString();
			return this;
		}


		public Builder withSlf4jMdcContextCorrelationId() {
			String id = MDC.get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY);
			if(id == null) {
				throw new IllegalStateException("Expected correlation id in MDC context");
			}
			return withCorrelationId(id);
		}

		public Builder withGrpcMdcContextCorrelationId() {
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

		public Builder withMdcContextCorrelationId() {
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


		public FixedCorrelationIdClientInterceptor build() {
			if(correlationId == null) {
				throw new IllegalStateException("Expected correlation id");
			}
			return new FixedCorrelationIdClientInterceptor(correlationId);
		}
	}

	private final String correlationId;

    public FixedCorrelationIdClientInterceptor(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
		return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

			@Override
			public void start(Listener<RespT> responseListener, Metadata metadata) {
				metadata.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
				super.start(responseListener, metadata);
			}
		};

	}
}
