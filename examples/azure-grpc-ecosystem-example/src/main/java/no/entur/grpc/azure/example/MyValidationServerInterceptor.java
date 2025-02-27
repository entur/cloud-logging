package no.entur.grpc.azure.example;

import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusException;
import io.grpc.protobuf.StatusProto;
import org.springframework.core.Ordered;

// https://github.com/envoyproxy/protoc-gen-validate/blob/master/java/pgv-java-grpc/src/main/java/io/envoyproxy/pgv/grpc/ValidatingServerInterceptor.java

public class MyValidationServerInterceptor implements ServerInterceptor, Ordered {

	protected final int order;

    public MyValidationServerInterceptor(int order) {
        this.order = order;
    }

    @Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(call, headers)) {
			private boolean aborted = false;

			public void onMessage(ReqT message) {
				if(call.getMethodDescriptor().getFullMethodName().endsWith("/greeting4")) {
					Status status = buildStatus(Code.INVALID_ARGUMENT, "My error message", new RuntimeException("My exception"));
					StatusException exception = StatusProto.toStatusException(status);
					aborted = true;
					
					call.close(exception.getStatus(), exception.getTrailers());
				} else {
					super.onMessage(message);
				}
			}

			public void onHalfClose() {
				if (!this.aborted) {
					super.onHalfClose();
				}

			}
		};
	}

	private static Status buildStatus(Code code, String message, Throwable cause) {

		Status.Builder b = Status.newBuilder()
				.setCode(code.getNumber())
				.setMessage(message);
		Any details = createDetails(cause);
		b.addDetails(details);

		return b.build();
	}

	private static Any createDetails(Throwable cause) {
		return Any.pack(BadRequest.newBuilder()
				.addFieldViolations(BadRequest.FieldViolation.newBuilder().setField("*").setDescription("Invalid message").build())
				.build());
	}

	public int getOrder() {
		return this.order;
	}
}
