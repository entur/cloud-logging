package no.entur.logging.cloud.grpc.trace;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.protobuf.ProtoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Interceptor for checking that the correlation-id is present in the incoming request.
 *
 */

public class CorrelationIdRequiredServerInterceptor implements ServerInterceptor {
	public static final String X_CORRELATION_ID_HEADER_IS_MISSING = "x-correlation-id header is missing";
	private static Logger LOGGER = LoggerFactory.getLogger(CorrelationIdRequiredServerInterceptor.class);
	public static final Metadata.Key<String> X_CORRELATION_ID_HEADER_KEY = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

	private static final Metadata.Key<String> USER_AGENT_KEY = Metadata.Key.of("user-agent", Metadata.ASCII_STRING_MARSHALLER);

	private static final Metadata.Key<Status> STATUS_DETAILS_KEY = Metadata.Key.of("grpc-status-details-bin", ProtoUtils.metadataMarshaller(Status.getDefaultInstance()));

	/**
	 *
	 * Interface for customization of logging in case of a missing correlation id
	 *
	 */

	public static interface CorrelationIdListener {

		<ReqT, RespT> void onCorrelationIdMissing(ServerCall<ReqT, RespT> call, Metadata m);
	}

	public static class DefaultCorrelationIdListener implements CorrelationIdListener {

		@Override
		public <ReqT, RespT> void onCorrelationIdMissing(ServerCall<ReqT, RespT> call, Metadata m) {
			String userAgent = m.get(USER_AGENT_KEY);
			if(userAgent != null) {
				LOGGER.warn("No correlation-id header in call from user-agent " + userAgent + ", returning status " + Code.INVALID_ARGUMENT_VALUE);
			} else {
				LOGGER.warn("No correlation-id header in call from unknown user-agent, returning status " + Code.INVALID_ARGUMENT_VALUE);
			}
		}
	}

	protected boolean required;

	protected final CorrelationIdListener correlationIdListener;

	public CorrelationIdRequiredServerInterceptor() {
		this(true, new DefaultCorrelationIdListener());
	}

	public CorrelationIdRequiredServerInterceptor(boolean required, CorrelationIdListener correlationIdListener) {
		this.required = required;
        this.correlationIdListener = correlationIdListener;
    }

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		if(required) {
			if (!headers.containsKey(X_CORRELATION_ID_HEADER_KEY)) {
				return handleMissingCorrelationId(call, headers);
			}
		}
		return next.startCall(call, headers);
	}


	protected <ReqT, RespT> ServerCall.Listener<ReqT> handleMissingCorrelationId(ServerCall<ReqT, RespT> call, Metadata m) {
		// log some message
		correlationIdListener.onCorrelationIdMissing(call, m);

		// https://stackoverflow.com/questions/73954274/what-is-the-proper-way-to-return-an-error-from-grpc-serverinterceptor
		Status statusProto = Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage(X_CORRELATION_ID_HEADER_IS_MISSING).build();

		Metadata metadata = new Metadata();
		metadata.put(STATUS_DETAILS_KEY, statusProto);

		call.close(io.grpc.Status.INVALID_ARGUMENT.withDescription(X_CORRELATION_ID_HEADER_IS_MISSING), metadata);
		return new ServerCall.Listener<ReqT>() {};
	}
}
