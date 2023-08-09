package no.entur.grpc.example;

import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcTraceMdcContextInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import org.lognet.springboot.grpc.GRpcService;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GRpc service with default interceptor chain
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@GRpcService(applyGlobalInterceptors = false, interceptors = {

		TransmitStatusRuntimeExceptionInterceptor.class,
		// Validation
		MyValidationServerInterceptor.class,
		// logging
		GrpcLoggingServerInterceptor.class,
		// Trace
		GrpcTraceMdcContextInterceptor.class, // add trace headers (correlation-id and such)
		// MDC
		GrpcMdcContextInterceptor.class, // init context-aware MDC
})
public @interface MyGrpcService {
}
