package no.entur.logging.cloud.spring.grpc.ecosystem;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.error.GrpcExceptionInterceptor;
import org.springframework.core.Ordered;

/**
 *
 * This interceptor duplicates the ecosystem exception interceptor so to handle all errors before the request-response logging
 * while still having error handling earlier in the interceptor chain (i.e. for handling authentication errors and so on).<br><br>
 *
 * This is useful when doing request-response between authentication and controller while using the ecosystem exception handling.
 */

public class RequestResponseGrpcExceptionHandlerInterceptor implements ServerInterceptor, Ordered {

    protected final int order;

    private final GrpcExceptionInterceptor delegate;

    public RequestResponseGrpcExceptionHandlerInterceptor(GrpcExceptionInterceptor delegate, int order) {
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
