package no.entur.logging.cloud.spring.ondemand.grpc.scope;

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
import no.entur.logging.cloud.appender.scope.LoggingScopeSink;
import org.springframework.core.Ordered;

import java.util.function.Predicate;

/**
 * Interceptor for adding MDC for a gRPC {@linkplain Context}.
 */

public class GrpcLoggingScopeContextInterceptor implements ServerInterceptor, Ordered {

	public static final Context.Key<LoggingScope> KEY_CONTEXT = Context.key("LOGGING_SCOPE_CONTEXT");

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private int order = Ordered.HIGHEST_PRECEDENCE;
		private LoggingScopeSink sink;

		private GrpcLoggingScopeFilters filters;

		private GrpcContextLoggingScopeFactory factory;

		public Builder withFactory(GrpcContextLoggingScopeFactory factory) {
			this.factory = factory;
			return this;
		}

		public Builder withSink(LoggingScopeSink sink) {
			this.sink = sink;
			return this;
		}

		public Builder withFilters(GrpcLoggingScopeFilters filters) {
			this.filters = filters;
			return this;
		}

		public Builder withOrder(int order) {
			this.order = order;
			return this;
		}

		public GrpcLoggingScopeContextInterceptor build() {
			if(sink == null) {
				throw new IllegalStateException();
			}
			if(filters == null) {
				throw new IllegalStateException();
			}
			if(factory == null) {
				throw new IllegalStateException();
			}
			return new GrpcLoggingScopeContextInterceptor(sink, filters, factory, order);
		}
	}

	private final LoggingScopeSink sink;

	private final GrpcLoggingScopeFilters filters;

	private final int order;

	private final GrpcContextLoggingScopeFactory factory;

	protected GrpcLoggingScopeContextInterceptor(LoggingScopeSink sink, GrpcLoggingScopeFilters filters, GrpcContextLoggingScopeFactory factory, int order) {
		// prefer to use builder
		this.sink = sink;
		this.filters = filters;
		this.factory = factory;
		this.order = order;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		MethodDescriptor<ReqT, RespT> methodDescriptor = call.getMethodDescriptor();
		String serviceName = methodDescriptor.getServiceName();
		String fullMethodName = methodDescriptor.getFullMethodName();

		GrpcLoggingScopeFilter filter = filters.getFilter(serviceName, fullMethodName);

		Predicate<ILoggingEvent> queuePredicate = filter.getQueuePredicate();
		Predicate<ILoggingEvent> ignorePredicate = filter.getIgnorePredicate();

		Predicate<Metadata> httpHeaderPresentPredicate = filter.getGrpcHeaderPresentPredicate();
		if(httpHeaderPresentPredicate.test(headers)) {
			queuePredicate = filter.getTroubleshootQueuePredicate();
			ignorePredicate = filter.getTroubleshootIgnorePredicate();
		}

		LoggingScope scope = factory.openScope(queuePredicate, ignorePredicate, filter.getLogLevelFailurePredicate());

		Context context = Context.current().withValue(KEY_CONTEXT, scope);

		ServerCall<ReqT, RespT> interceptCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
			public void close(Status status, Metadata trailers) {
				try {
					if (filter.getGrpcStatusPredicate().test(status)) {
						// was there an error response
						sink.write(scope);
					} else if(scope.isFailure()) {
						// there some dangerous error message
						sink.write(scope);
					}
				} finally {
					factory.closeScope(scope); // this is really a noop operation
				}

				super.close(status, trailers);
			}
		};

		return Contexts.interceptCall(context, interceptCall, headers, next);
	}

	@Override
	public int getOrder() {
		return order;
	}

}
