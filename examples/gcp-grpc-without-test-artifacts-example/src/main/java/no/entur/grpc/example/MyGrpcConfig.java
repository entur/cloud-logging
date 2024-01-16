package no.entur.grpc.example;

import io.grpc.ServerInterceptor;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyGrpcConfig {

    // interceptors:
    // 50: CorrelationIdGrpcMdcContextServerInterceptor
    //  - cloud-logging via no.entur.logging.grpc.trace.mdc.interceptor-order
    // 100: lognet recovery - GRpcExceptionHandlerInterceptor for auth errors
    //  - grpc.recovery.interceptor-order
    // 275: GrpcLoggingServerInterceptor - request-response logging via
    //  - entur.logging.request-response.grpc.server.interceptor-order
    // 280: lognet recovery clone / handle most errors here
    //  - entur.logging.request-response.grpc.server.exception-handler.interceptor-order
    // 500: MyValidationServerInterceptor

    @Bean
    public ServerInterceptor myValidationServerInterceptor() {
        return new MyValidationServerInterceptor(500);
    }

    @Bean
    public GrpcServerLoggingFilters grpcServerLoggingFilters() {
        return GrpcServerLoggingFilters
                .newBuilder()
                .classicDefaultLogging()
                .fullLoggingForService(GreetingServiceGrpc.SERVICE_NAME, GreetingServiceGrpc.getGreeting3Method()) //  /greeting3
                .fullLoggingForService(GreetingServiceGrpc.SERVICE_NAME, GreetingServiceGrpc.getFullLoggingMethod()) //  /fullLogging
                .summaryLoggingForService(GreetingServiceGrpc.SERVICE_NAME, GreetingServiceGrpc.getSummaryLoggingMethod()) // /summaryLogging
                .noLoggingForService(GreetingServiceGrpc.SERVICE_NAME, GreetingServiceGrpc.getNoLoggingMethod()) // /noLogging
                .build();
    }

}
