package no.entur.logging.cloud.grpc.trace;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusException;
import io.grpc.protobuf.StatusProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationIdValidationServerInterceptor implements ServerInterceptor {
	private static Logger logger = LoggerFactory.getLogger(CorrelationIdValidationServerInterceptor.class);
	public static final Metadata.Key<String> X_CORRELATION_ID_HEADER_KEY = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
	private boolean validationEnabled;


	public CorrelationIdValidationServerInterceptor(boolean validationEnabled) {
		this.validationEnabled = validationEnabled;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(call, headers)) {
			private boolean aborted = false;

			@Override
			public void onMessage(ReqT message) {
				try {
					if (!headers.containsKey(X_CORRELATION_ID_HEADER_KEY)) {
						if(validationEnabled){
							aborted = true;
							Status status = Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("x-correlation-id header is missing").build();
							StatusException statusException = StatusProto.toStatusException(status);
							call.close(statusException.getStatus(), statusException.getTrailers());
							return;
						}
						else{
							logger.info("Request is missing x-correlation-id header");
						}
					}
					super.onMessage(message);
				} catch (Throwable t) {
					Status status = Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(t.getMessage()).build();
					StatusException statusException = StatusProto.toStatusException(status);
					aborted = true;
					call.close(statusException.getStatus(), statusException.getTrailers());
				}
			}

			@Override
			public void onHalfClose() {
				if (!aborted) {
					super.onHalfClose();
				}
			}
		};

	}

}
