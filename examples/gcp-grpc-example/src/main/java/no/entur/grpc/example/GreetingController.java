package no.entur.grpc.example;


import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcTraceMdcContextInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import org.entur.grpc.example.GreetingResponse;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@GRpcService(applyGlobalInterceptors = false, interceptors = {

		TransmitStatusRuntimeExceptionInterceptor.class,
		// Validation
		MyValidationServerInterceptor.class,
		// logging
		GrpcLoggingServerInterceptor.class,
		// Trace
		GrpcTraceMdcContextInterceptor.class, // add trace headers (correlation-id and such)
		// MDC
		GrpcMdcContextInterceptor.class // init context-aware MDC
})
@Profile("!ondemand")
public class GreetingController extends AbstractGreetingController {

}