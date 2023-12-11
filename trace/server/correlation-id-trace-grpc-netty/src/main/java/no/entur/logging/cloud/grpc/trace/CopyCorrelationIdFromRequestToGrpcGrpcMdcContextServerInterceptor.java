package no.entur.logging.cloud.grpc.trace;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.*;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

/**
 *
 * Also adds request id
 *
 */

public class CopyCorrelationIdFromRequestToGrpcGrpcMdcContextServerInterceptor implements ServerInterceptor {

	private static final Metadata.Key<Status> STATUS_DETAILS_KEY = Metadata.Key.of("grpc-status-details-bin", ProtoLiteUtils.metadataMarshaller(Status.getDefaultInstance()));

	private static final Logger log = LoggerFactory.getLogger(CopyCorrelationIdFromRequestToGrpcGrpcMdcContextServerInterceptor.class);

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private boolean required = false;

		private boolean create = true;

		public Builder withRequired(boolean required) {
			this.required = required;
			return this;
		}

		public Builder withCreate(boolean create) {
			this.create = create;
			return this;
		}

		public CopyCorrelationIdFromRequestToGrpcGrpcMdcContextServerInterceptor build() {
			return new CopyCorrelationIdFromRequestToGrpcGrpcMdcContextServerInterceptor(required, create);
		}

	}

	private boolean required;
	private boolean create;

	public CopyCorrelationIdFromRequestToGrpcGrpcMdcContextServerInterceptor(boolean required, boolean create) {
		this.required = required;
		this.create = create;
	}

	protected CopyCorrelationIdFromRequestToGrpcGrpcMdcContextServerInterceptor() {
		this(false, true);
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata m, ServerCallHandler<ReqT, RespT> next) {
		// omit cleanup, fields will die as part of the mdc context
		String correlationId = getCorrelationId(m);

		if (correlationId == null && required) {
			// https://stackoverflow.com/questions/73954274/what-is-the-proper-way-to-return-an-error-from-grpc-serverinterceptor
			Status statusProto = Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("x-correlation-id header is invalid").build();

			Metadata metadata = new Metadata();
			metadata.put(STATUS_DETAILS_KEY, statusProto);

			call.close(io.grpc.Status.INVALID_ARGUMENT, metadata);
			return new ServerCall.Listener<ReqT>() {};
		}

		if(correlationId == null && create) {
			correlationId = UUID.randomUUID().toString();
		}

		GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
		if(grpcMdcContext == null) {
			// create new context
			grpcMdcContext = new GrpcMdcContext();

			if(correlationId != null) {
				grpcMdcContext.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY, correlationId);
			}
			grpcMdcContext.put(CorrelationIdGrpcMdcContext.REQUEST_ID_MDC_KEY, UUID.randomUUID().toString());

			Context context = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, grpcMdcContext);
			return Contexts.interceptCall(context, call, m, next);
		}

		if(correlationId != null) {
			grpcMdcContext.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY, correlationId);
		}
		grpcMdcContext.put(CorrelationIdGrpcMdcContext.REQUEST_ID_MDC_KEY, UUID.randomUUID().toString());

		// no new context necessary
		return next.startCall(call, m);

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
}