package no.entur.grpc.example;


import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.grpc.mdc.InitializeGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.context.annotation.Profile;

@GRpcService(applyGlobalInterceptors = false, interceptors = {
		// Validation
		MyValidationServerInterceptor.class,
		// logging
		GrpcLoggingServerInterceptor.class,
		// Trace
		CorrelationIdGrpcMdcContextServerInterceptor.class, // add trace headers (correlation-id and such)


})
@Profile("!ondemand")
public class GreetingController extends AbstractGreetingController {

}