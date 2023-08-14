package no.entur.grpc.example;


import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextInterceptor;
import no.entur.logging.cloud.grpc.mdc.scope.GrpcLoggingScopeContextInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcTraceMdcContextInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.context.annotation.Profile;

@Profile("ondemand")
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

		GrpcLoggingScopeContextInterceptor.class
})
public class GreetingControllerWithOnDemandLogging extends AbstractGreetingController {

}