package no.entur.grpc.example;


import no.entur.logging.cloud.gcp.trace.spring.grpc.interceptor.OrderedTraceIdGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.spring.rr.grpc.OrderedGrpcLoggingServerInterceptor;
import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcExceptionHandlerInterceptor;
import no.entur.logging.cloud.trace.spring.grpc.interceptor.OrderedCorrelationIdGrpcMdcContextServerInterceptor;
import org.springframework.context.annotation.Profile;
import org.springframework.grpc.server.service.GrpcService;

// note: order is reversed compared to lognet
@GrpcService(interceptors = {
		// Trace
		OrderedCorrelationIdGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)
		OrderedTraceIdGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)

		// logging
		OrderedGrpcLoggingServerInterceptor.class,
		RequestResponseGrpcExceptionHandlerInterceptor.class,

		// Validation
		MyValidationServerInterceptor.class,

})

@Profile("!ondemand")
public class GreetingController extends AbstractGreetingController {

}