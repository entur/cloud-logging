package no.entur.logging.cloud.grpc.trace;

import io.grpc.*;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

/**
 *
 * Also adds request id
 *
 */

public class CorrelationIdGrpcMdcContextServerInterceptor extends CorrelationIdRequiredServerInterceptor {

	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 *
	 * Simple interface to facilitate logging; extracting one or more parameters to identity the request source.
	 *
	 */

	public static class Builder {
		private boolean required = false;

		private boolean response = true;

		private CorrelationIdListener correlationIdListener;

		public Builder withResponse(boolean response) {
			this.response = response;
			return this;
		}

		public Builder withRequired(boolean required) {
			this.required = required;
			return this;
		}

		public Builder withCorrelationIdListener(CorrelationIdListener correlationIdListener) {
			this.correlationIdListener = correlationIdListener;
			return this;
		}

		public CorrelationIdGrpcMdcContextServerInterceptor build() {
			if(required && correlationIdListener == null) {
				correlationIdListener = new DefaultCorrelationIdListener();
			}
			return new CorrelationIdGrpcMdcContextServerInterceptor(required, response, correlationIdListener);
		}

	}

	protected final boolean response;

	public CorrelationIdGrpcMdcContextServerInterceptor(boolean required, boolean response, CorrelationIdListener correlationIdListener) {
		super(required, correlationIdListener);
		this.response = response;
    }

	public CorrelationIdGrpcMdcContextServerInterceptor() {
		this(false, true, null);
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata m, ServerCallHandler<ReqT, RespT> next) {
		// omit cleanup, fields will die as part of the mdc context
		String correlationId = getCorrelationId(m);

		if (correlationId == null && required) {
			return handleMissingCorrelationId(call, m);
		}

		if(correlationId == null) {
			correlationId = UUID.randomUUID().toString();
		}

		GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
		if(grpcMdcContext == null) {
			return startCallInNewGrpcMdcContext(call, m, next, correlationId);
		}

		if(correlationId != null) {
			grpcMdcContext.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY, correlationId);
		}
		grpcMdcContext.put(CorrelationIdGrpcMdcContext.REQUEST_ID_MDC_KEY, UUID.randomUUID().toString());

		if(response) {
			call = new AddCorrelationIdToResponseServerCall<>(call, correlationId);
		}
		// no new context necessary
		return next.startCall(call, m);
	}

	protected <ReqT, RespT> ServerCall.Listener<ReqT> startCallInNewGrpcMdcContext(ServerCall<ReqT, RespT> call, Metadata m, ServerCallHandler<ReqT, RespT> next, String correlationId) {
		// create new context
		GrpcMdcContext grpcMdcContext = new GrpcMdcContext();

		if(correlationId != null) {
			grpcMdcContext.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY, correlationId);
		}
		grpcMdcContext.put(CorrelationIdGrpcMdcContext.REQUEST_ID_MDC_KEY, UUID.randomUUID().toString());

		Context context = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, grpcMdcContext);

		if(response) {
			call = new AddCorrelationIdToResponseServerCall<>(call, correlationId);
		}

		return Contexts.interceptCall(context, call, m, next);
	}


	private String getCorrelationId(Metadata m) {
		String header = m.get(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER_KEY);
		if (header != null && !header.isBlank()) {
			return sanitize(header);
		}
		return null;
	}

	public static String sanitize(String inputValue) {
		if (!containsNumbersLowercaseLettersAndDashes(inputValue)) {
			try {
				return URLEncoder.encode(inputValue, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e); // should never happen
			}
		}
		return inputValue;
	}

	public static boolean containsNumbersLowercaseLettersAndDashes(String inputValue) {
		for (int i = 0; i < inputValue.length(); i++) {
			char c = inputValue.charAt(i);
			if (!Character.isDigit(c) && c != '-' && (c < 'a' || c > 'z')) {
				return false;
			}
		}
		return true;
	}


	private static class AddCorrelationIdToResponseServerCall<ReqT, RespT> extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {
		private final String correlationId;

		public AddCorrelationIdToResponseServerCall(ServerCall<ReqT, RespT> delegate, String correlationId) {
			super(delegate);
			this.correlationId = correlationId;
		}

		@Override
		public void sendHeaders(Metadata metadata) {
			// this happens before all the responses
			metadata.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
			super.sendHeaders(metadata);
		}

		@Override
		public void close(io.grpc.Status status, Metadata trailers) {
			trailers.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
			super.close(status, trailers);
		}

    }
}