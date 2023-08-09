package no.entur.grpc.example;

import io.grpc.ServerInterceptor;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyGrpcConfig {

    @Bean
    public ServerInterceptor myValidationServerInterceptor() {
        return new MyValidationServerInterceptor();
    }

    @Bean
    public ServerInterceptor transmitStatusRuntimeExceptionInterceptor() {
        return TransmitStatusRuntimeExceptionInterceptor.instance();
    }

    @Bean
    public GrpcServerLoggingFilters grpcServerLoggingFilters() {

        String serviceName = "/" + GreetingServiceGrpc.SERVICE_NAME;

        return GrpcServerLoggingFilters
                .newBuilder()
                .classicDefaultLogging()
                .fullLoggingForPrefix(serviceName + "/greeting3")
                .fullLoggingForPrefix(serviceName + "/fullLogging")
                .summaryLoggingForPrefix(serviceName + "/summaryLogging")
                .noLoggingForPrefix(serviceName + "/noLogging")
                .build();
    }

}
