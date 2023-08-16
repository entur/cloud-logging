package no.entur.logging.cloud.gcp.spring.grpc.lognet.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
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

		private GrpcLoggingScopeFilters filters;

		public Builder withAppender(LoggingScopeAsyncAppender appender) {
			this.appender = appender;
			return this;
		}

		public Builder withFilters(GrpcLoggingScopeFilters filters) {
			this.filters = filters;
			return this;
		}

		public GrpcLoggingScopeContextInterceptor build() {
			if(appender == null) {
				throw new IllegalStateException();
			}
			if(filters == null) {
				throw new IllegalStateException();
			}
			return new GrpcLoggingScopeContextInterceptor(appender, filters);
		}
	}

	private final LoggingScopeAsyncAppender appender;

	private final GrpcLoggingScopeFilters filters;

	protected GrpcLoggingScopeContextInterceptor(LoggingScopeAsyncAppender appender, GrpcLoggingScopeFilters filters) {
		// prefer to use builder
		this.appender = appender;
		this.filters = filters;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		MethodDescriptor<ReqT, RespT> methodDescriptor = call.getMethodDescriptor();
		String serviceName = methodDescriptor.getServiceName();
		String fullMethodName = methodDescriptor.getFullMethodName();

		GrpcLoggingScopeFilter filter = filters.getFilter(serviceName, fullMethodName);

		LoggingScopeFactory<Context> loggingScopeFactory = appender.getLoggingScopeFactory();

		Context context = loggingScopeFactory.openScope(filter.getQueuePredicate(), filter.getIgnorePredicate());

		ServerCall<ReqT, RespT> interceptCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
			public void close(Status status, Metadata trailers) {
				LoggingScope scope = loggingScopeFactory.getScope();
				if(scope != null) {
					try {
						if (filter.getGrpcStatusPredicate().test(status)) {
							// was there an error response
							appender.flushScope();
						} else {
							// was there some dangerous error message?
							Predicate<ILoggingEvent> logLevelFailurePredicate = filter.getLogLevelFailurePredicate();
							ConcurrentLinkedQueue<ILoggingEvent> events = scope.getEvents();
							for (ILoggingEvent event : events) {
								if (logLevelFailurePredicate.test(event)) {
									appender.flushScope();
									break;
								}
							}
						}
					} finally {
						appender.closeScope(); // this is really a noop operation
					}

					super.close(status, trailers);
				} else {
					super.close(status, trailers);
				}
			}
		};

		return Contexts.interceptCall(context, interceptCall, headers, next);
	}

}
