package no.entur.logging.cloud.gcp.spring.gcp.grpc.lognet;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.filter.GrpcClientLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerInterceptor;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerMethodResolver;
import org.springframework.core.Ordered;

/**
 *
 * This interceptor duplicates the lognet recovery interceptor so to handle all errors before the request-response logging
 * while still having error handling earlier in the interceptor chain (i.e. for handling authentication errors and so on).<br><br>
 *
 * This is useful when doing request-response between authentication and controller while using the lognet exception handling.
 */

public class RequestResponseGRpcExceptionHandlerInterceptor implements ServerInterceptor, Ordered {

    protected final int order;

    private final GRpcExceptionHandlerInterceptor delegate;

    public RequestResponseGRpcExceptionHandlerInterceptor(GRpcExceptionHandlerInterceptor delegate, int order) {
        this.delegate = delegate;
        this.order = order;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        return delegate.interceptCall(serverCall, metadata, serverCallHandler);
    }

    @Override
    public int getOrder() {
        return order;
    }

}
