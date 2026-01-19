package no.entur.logging.cloud.spring.grpc.spring;

import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import no.entur.logging.cloud.spring.rr.grpc.OrderedGrpcLoggingServerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.grpc.server.autoconfigure.exception.GrpcExceptionHandlerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * We need the proper error handling to run before the response is logged, i.e. to make sure no exception is thrown through the
 * request-response interceptor, we wrap the system error handler to run in an additional interceptor and make it run first.
 * .
 *
 */

@Configuration
@AutoConfigureBefore(GrpcExceptionHandlerAutoConfiguration.class)
public class RequestResponseGrpcSpringAutoConfiguration {

    @Value("${entur.logging.request-response.grpc.server.interceptor-order:5175}")
    private int serverInterceptorOrder;

    @Bean
    @ConditionalOnMissingBean(OrderedGrpcLoggingServerInterceptor.class)
    public OrderedGrpcLoggingServerInterceptor orderedGrpcLoggingServerInterceptor(GrpcPayloadJsonMapper grpcPayloadJsonMapper, GrpcMetadataJsonMapper grpcMetadataJsonMapper, GrpcSink grpcSink, GrpcServerLoggingFilters grpcServerLoggingFilters) {
        return new OrderedGrpcLoggingServerInterceptor(grpcSink, grpcServerLoggingFilters, grpcMetadataJsonMapper, grpcPayloadJsonMapper, serverInterceptorOrder);
    }

    @Bean
    @ConditionalOnProperty(name = {"entur.logging.request-response.grpc.server.exception-handler.enabled"}, havingValue = "true", matchIfMissing = true)
    public StatusRuntimeExceptionExceptionHandler statusRuntimeExceptionExceptionHandler() {
        return new StatusRuntimeExceptionExceptionHandler(0);
    }

    @Bean
    @ConditionalOnProperty(name = {"entur.logging.request-response.grpc.server.exception-handler.enabled"}, havingValue = "true", matchIfMissing = true)
    public RuntimeExceptionExceptionHandler runtimeExceptionExceptionHandler() {
        return new RuntimeExceptionExceptionHandler(0);
    }

}