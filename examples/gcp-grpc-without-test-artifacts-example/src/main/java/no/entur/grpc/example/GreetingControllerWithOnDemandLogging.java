package no.entur.grpc.example;


import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.gcp.spring.gcp.grpc.lognet.RequestResponseGRpcExceptionHandlerInterceptor;
import no.entur.logging.cloud.gcp.spring.ondemand.grpc.lognet.scope.GrpcLoggingScopeContextInterceptor;
import no.entur.logging.cloud.grpc.mdc.InitializeGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerInterceptor;
import org.springframework.context.annotation.Profile;

@Profile("ondemand")
@GRpcService(applyGlobalInterceptors = false, interceptors = {

		// Validation
		MyValidationServerInterceptor.class,

		// logging
		RequestResponseGRpcExceptionHandlerInterceptor.class,
		GrpcLoggingServerInterceptor.class,

		GRpcExceptionHandlerInterceptor.class,

		// Trace
		CorrelationIdGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)

		GrpcLoggingScopeContextInterceptor.class
})
public class GreetingControllerWithOnDemandLogging extends AbstractGreetingController {

}