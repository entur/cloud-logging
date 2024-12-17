package no.entur.grpc.example;


import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.spring.grpc.lognet.OrderedGrpcLoggingServerInterceptor;
import no.entur.logging.cloud.spring.grpc.lognet.RequestResponseGrpcExceptionHandlerInterceptor;
import no.entur.logging.cloud.grpc.mdc.InitializeGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
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