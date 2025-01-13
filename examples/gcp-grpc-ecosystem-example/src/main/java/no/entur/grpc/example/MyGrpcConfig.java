package no.entur.grpc.example;

import io.grpc.ServerInterceptor;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyGrpcConfig {

    @Bean
    public ServerInterceptor myValidationServerInterceptor() {
        return new MyValidationServerInterceptor(6000);
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
