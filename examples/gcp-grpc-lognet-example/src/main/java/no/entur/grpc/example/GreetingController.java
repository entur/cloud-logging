package no.entur.grpc.example;


import no.entur.logging.cloud.spring.rr.grpc.OrderedGrpcLoggingServerInterceptor;
import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcExceptionHandlerInterceptor;
import no.entur.logging.cloud.trace.spring.grpc.interceptor.OrderedCorrelationIdGrpcMdcContextServerInterceptor;
import org.lognet.springboot.grpc.GRpcService;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandlerInterceptor;
import org.springframework.context.annotation.Profile;

@GRpcService(applyGlobalInterceptors = false, interceptors = {
		// Validation
		MyValidationServerInterceptor.class,

		// logging
		RequestResponseGrpcExceptionHandlerInterceptor.class,
		OrderedGrpcLoggingServerInterceptor.class,

		GRpcExceptionHandlerInterceptor.class,

		// Trace
		OrderedCorrelationIdGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)
})
@Profile("!ondemand")
public class GreetingController extends AbstractGreetingController {

}