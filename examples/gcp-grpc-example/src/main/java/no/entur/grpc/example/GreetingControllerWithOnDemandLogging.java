package no.entur.grpc.example;


import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.gcp.spring.grpc.lognet.scope.GrpcLoggingScopeContextInterceptor;
import no.entur.logging.cloud.grpc.mdc.InitializeGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.grpc.trace.CopyTraceFromRequestToGrpcGrpcMdcContextServerServerInterceptor;
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
		CopyTraceFromRequestToGrpcGrpcMdcContextServerServerInterceptor.class, // add trace headers (correlation-id and such)
		// MDC
		InitializeGrpcMdcContextServerInterceptor.class, // init context-aware MDC

		GrpcLoggingScopeContextInterceptor.class
})
public class GreetingControllerWithOnDemandLogging extends AbstractGreetingController {

}