package no.entur.logging.cloud.grpc.mdc.scope;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;

import java.util.function.Predicate;

/**
 * Interceptor for adding MDC for a gRPC {@linkplain Context}.
 */

public class GrpcLoggingScopeContextInterceptor implements ServerInterceptor {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private LoggingScopeAsyncAppender appender;

		private Predicate<Status> predicate;

		public Builder withAppender(LoggingScopeAsyncAppender appender) {
			this.appender = appender;
			return this;
		}

		public Builder withPredicate(Predicate<Status> predicate) {
			this.predicate = predicate;
			return this;
		}

		public GrpcLoggingScopeContextInterceptor build() {
			if(appender == null) {
				throw new IllegalStateException();
			}
			if(predicate == null) {
				throw new IllegalStateException();
			}
			return new GrpcLoggingScopeContextInterceptor(appender, predicate);
		}
	}

	private LoggingScopeAsyncAppender appender;

	private Predicate<Status> predicate;

	protected GrpcLoggingScopeContextInterceptor(LoggingScopeAsyncAppender appender, Predicate<Status> predicate) {
		// prefer to use builder
		this.appender = appender;
		this.predicate = predicate;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		Context context = appender.openScope();

		ServerCall<ReqT, RespT> interceptCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
			public void close(Status status, Metadata trailers) {
				if(predicate.test(status)) {
					appender.flushScope();

					super.close(status, trailers);
				} else {
					super.close(status, trailers);
				}
			}
		};

		return Contexts.interceptCall(context, interceptCall, headers, next);
	}

}
