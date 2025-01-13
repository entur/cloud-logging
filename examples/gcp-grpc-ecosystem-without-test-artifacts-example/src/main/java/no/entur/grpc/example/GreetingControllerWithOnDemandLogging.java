package no.entur.grpc.example;


import net.devh.boot.grpc.server.service.GrpcService;
import no.entur.logging.cloud.spring.ondemand.grpc.scope.GrpcLoggingScopeContextInterceptor;
import no.entur.logging.cloud.spring.rr.grpc.OrderedGrpcLoggingServerInterceptor;
import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcExceptionHandlerInterceptor;
import no.entur.logging.cloud.trace.spring.grpc.interceptor.OrderedCorrelationIdGrpcMdcContextServerInterceptor;
import org.springframework.context.annotation.Profile;

@Profile("ondemand")
@GrpcService(interceptors = {
		GrpcLoggingScopeContextInterceptor.class,
		// Trace
		OrderedCorrelationIdGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)

		// logging
		OrderedGrpcLoggingServerInterceptor.class,
		RequestResponseGrpcExceptionHandlerInterceptor.class,

		// Validation
		MyValidationServerInterceptor.class,
}, sortInterceptors = true)
public class GreetingControllerWithOnDemandLogging extends AbstractGreetingController {

}