package no.entur.logging.cloud.gcp.spring.gcp.grpc.lognet;

import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerInterceptor;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerMethodResolver;

/**
 *
 * This interceptor duplicates the lognet recovery interceptor so to handle all errors before the request-response logging
 * while still having error handling earlier in the interceptor chain (i.e. for handling authentication errors and so on).<br><br>
 *
 * This is useful when doing request-response between authentication and controller while using the lognet exception handling.
 */

public class RequestResponseGRpcExceptionHandlerInterceptor extends GRpcExceptionHandlerInterceptor {
    public RequestResponseGRpcExceptionHandlerInterceptor(GRpcExceptionHandlerMethodResolver methodResolver, FailureHandlingSupport failureHandlingSupport, GRpcServerProperties serverProperties) {
        super(methodResolver, failureHandlingSupport, serverProperties);
    }
}
