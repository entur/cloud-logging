package no.entur.grpc.example;


import net.devh.boot.grpc.server.service.GrpcService;
import no.entur.logging.cloud.spring.grpc.ecosystem.OrderedGrpcLoggingServerInterceptor;
import no.entur.logging.cloud.spring.grpc.ecosystem.RequestResponseGrpcExceptionHandlerInterceptor;
import no.entur.logging.cloud.spring.ondemand.grpc.scope.GrpcLoggingScopeContextInterceptor;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
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
})
public class GreetingControllerWithOnDemandLogging extends AbstractGreetingController {

}