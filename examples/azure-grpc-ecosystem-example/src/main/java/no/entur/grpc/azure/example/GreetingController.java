package no.entur.grpc.azure.example;


import net.devh.boot.grpc.server.service.GrpcService;
import no.entur.logging.cloud.spring.grpc.ecosystem.OrderedGrpcLoggingServerInterceptor;
import no.entur.logging.cloud.spring.grpc.ecosystem.RequestResponseGrpcExceptionHandlerInterceptor;
import no.entur.logging.cloud.trace.spring.grpc.interceptor.OrderedCorrelationIdGrpcMdcContextServerInterceptor;
import org.springframework.context.annotation.Profile;

// note: order is reversed compared to lognet
@GrpcService(interceptors = {
		// Trace
		OrderedCorrelationIdGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)

		// logging
		OrderedGrpcLoggingServerInterceptor.class,
		RequestResponseGrpcExceptionHandlerInterceptor.class,

		// Validation
		MyValidationServerInterceptor.class,

}, sortInterceptors = false)

@Profile("!ondemand")
public class GreetingController extends AbstractGreetingController {

}