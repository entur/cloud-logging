package no.entur.grpc.example;


import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.grpc.mdc.InitializeGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.grpc.trace.CopyCorrelationIdFromRequestToGrpcGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.context.annotation.Profile;

@GRpcService(applyGlobalInterceptors = false, interceptors = {

		TransmitStatusRuntimeExceptionInterceptor.class,
		// Validation
		MyValidationServerInterceptor.class,
		// logging
		GrpcLoggingServerInterceptor.class,
		// Trace
		CopyCorrelationIdFromRequestToGrpcGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)
		// MDC
		InitializeGrpcMdcContextServerInterceptor.class // init context-aware MDC
})
@Profile("!ondemand")
public class GreetingController extends AbstractGreetingController {

}