package org.entur.logging.grpc.mdc;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.BiFunction;

public class TestMdcContextInterceptor implements ServerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(TestMdcContextInterceptor.class);

	public TestMdcContextInterceptor() {
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
		if (grpcMdcContext != null) {
			grpcMdcContext.put("testKey", "testValueFromInterceptor");

			ServerCall<ReqT, RespT> interceptCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
				public void close(Status status, Metadata trailers) {
					try {
						super.close(status, trailers);
					} finally {
						grpcMdcContext.remove("testKey");
					}
				}
			};
			return next.startCall(interceptCall, headers);
		} else {
			// ignore
			log.warn("Unable to set trace MDC context, please add " + GrpcMdcContextInterceptor.class.getName() + " before this interceptor");
			return next.startCall(call, headers);
		}
	}

}
