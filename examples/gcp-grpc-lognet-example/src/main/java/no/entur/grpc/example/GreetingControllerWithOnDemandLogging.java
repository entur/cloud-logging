package no.entur.grpc.example;


import no.entur.logging.cloud.spring.grpc.lognet.RequestResponseGrpcExceptionHandlerInterceptor;
import no.entur.logging.cloud.spring.ondemand.grpc.lognet.scope.GrpcLoggingScopeContextInterceptor;
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
		RequestResponseGrpcExceptionHandlerInterceptor.class,
		GrpcLoggingServerInterceptor.class,

		GRpcExceptionHandlerInterceptor.class,

		// Trace
		CorrelationIdGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)

		GrpcLoggingScopeContextInterceptor.class
})
public class GreetingControllerWithOnDemandLogging extends AbstractGreetingController {

}