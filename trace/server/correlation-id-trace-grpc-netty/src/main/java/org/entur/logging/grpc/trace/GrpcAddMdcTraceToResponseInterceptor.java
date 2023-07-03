package org.entur.logging.grpc.trace;

import io.grpc.*;
import org.entur.logging.grpc.mdc.GrpcMdcContext;

public class GrpcAddMdcTraceToResponseInterceptor implements ServerInterceptor {

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(new AddTrailerToServerCall(call), headers)) {
		};
	}

	private class AddTrailerToServerCall<ReqT, RespT> extends ServerCall<ReqT, RespT> {
		private ServerCall<ReqT, RespT> call;

		public AddTrailerToServerCall(ServerCall<ReqT, RespT> call) {
			this.call = call;
		}

		@Override
		public void request(int i) {
			call.request(i);
		}

		@Override
		public void sendHeaders(Metadata metadata) {
			// this happens before all the responses
			try {
				GrpcMdcContext context = GrpcMdcContext.get();

				// does not make much sense to create this value if it is not already present
				// however it should always be present
				if (context != null) {
					String correlationId = context.get(GrpcTraceMdcContext.CORRELATION_ID_MDC_KEY);
					if (correlationId != null) {
						metadata.put(GrpcTraceMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
					}
				}
			} finally {
				call.sendHeaders(metadata);
			}
		}

		@Override
		public void sendMessage(RespT respT) {
			call.sendMessage(respT);
		}

		@Override
		public void close(Status status, Metadata metadata) {
			// this happens after the last message
			try {
				GrpcMdcContext context = GrpcMdcContext.get();

				// does not make much sense to create this value if it is not already present
				// however it should always be present
				if (context != null) {
					String correlationId = context.get(GrpcTraceMdcContext.CORRELATION_ID_MDC_KEY);
					if (correlationId != null) {
						metadata.put(GrpcTraceMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
					}
				}
			} finally {
				call.close(status, metadata);
			}
		}

		@Override
		public boolean isCancelled() {
			return call.isCancelled();
		}

		@Override
		public MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
			return call.getMethodDescriptor();
		}
	}
}
