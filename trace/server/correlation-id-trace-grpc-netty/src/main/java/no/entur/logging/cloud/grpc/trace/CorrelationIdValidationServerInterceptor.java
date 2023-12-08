package no.entur.logging.cloud.grpc.trace;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationIdValidationServerInterceptor implements ServerInterceptor {
	private static Logger logger = LoggerFactory.getLogger(CorrelationIdValidationServerInterceptor.class);
	public static final Metadata.Key<String> X_CORRELATION_ID_HEADER_KEY = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

	private static final Metadata.Key<Status> STATUS_DETAILS_KEY = Metadata.Key.of("grpc-status-details-bin", ProtoLiteUtils.metadataMarshaller(Status.getDefaultInstance()));

	private boolean enabled;

	public CorrelationIdValidationServerInterceptor() {
		this(true);
	}

	public CorrelationIdValidationServerInterceptor(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		if (!headers.containsKey(X_CORRELATION_ID_HEADER_KEY)) {
			if(enabled) {
				// https://stackoverflow.com/questions/73954274/what-is-the-proper-way-to-return-an-error-from-grpc-serverinterceptor
				Status statusProto = Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("x-correlation-id header is invalid").build();

				Metadata metadata = new Metadata();
				metadata.put(STATUS_DETAILS_KEY, statusProto);

				call.close(io.grpc.Status.INVALID_ARGUMENT, metadata);
				return new ServerCall.Listener<ReqT>() {
				};
			}
			logger.info("Request is missing x-correlation-id header");
		}
		return next.startCall(call, headers);
	}

}
