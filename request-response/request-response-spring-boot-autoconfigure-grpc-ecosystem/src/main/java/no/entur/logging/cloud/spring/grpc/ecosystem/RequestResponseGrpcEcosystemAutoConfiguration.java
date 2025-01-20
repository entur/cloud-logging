package no.entur.logging.cloud.spring.grpc.ecosystem;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration;
import net.devh.boot.grpc.server.error.GrpcExceptionInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import no.entur.logging.cloud.spring.rr.grpc.OrderedGrpcLoggingServerInterceptor;
import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcExceptionHandlerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 *
 * We need the proper error handling to run before the response is logged, i.e. to make sure no exception is thrown through the
 * request-response interceptor, we wrap the system error handler to run in an additional interceptor and make it run first.
 * .
 *
 */

@Configuration
@AutoConfigureBefore(GrpcAdviceAutoConfiguration.class)
public class RequestResponseGrpcEcosystemAutoConfiguration {

    @Value("${entur.logging.request-response.grpc.server.exception-handler.interceptor-order:0}")
    private int exceptionInterceptorOrder;

    @Value("${entur.logging.request-response.grpc.server.interceptor-order:5175}")
    private int serverInterceptorOrder;

    @Bean
    @ConditionalOnMissingBean(OrderedGrpcLoggingServerInterceptor.class)
    public OrderedGrpcLoggingServerInterceptor orderedGrpcLoggingServerInterceptor(GrpcPayloadJsonMapper grpcPayloadJsonMapper, GrpcMetadataJsonMapper grpcMetadataJsonMapper, GrpcSink grpcSink, GrpcServerLoggingFilters grpcServerLoggingFilters) {
        return new OrderedGrpcLoggingServerInterceptor(grpcSink, grpcServerLoggingFilters, grpcMetadataJsonMapper, grpcPayloadJsonMapper, serverInterceptorOrder);
    }

    @GrpcAdvice
    @ConditionalOnProperty(name = {"entur.logging.request-response.grpc.server.exception-handler.enabled"}, havingValue = "true", matchIfMissing = true)
    public static class StatusRuntimeExceptionGrpcServiceAdvice {
        @java.lang.SuppressWarnings("all")

        // this error mapper can be overriden by specifying value in the annotation
        @GrpcExceptionHandler
        public Status handle(StatusRuntimeException e) {
            return e.getStatus();
        }
    }

    @Bean
    @ConditionalOnProperty(name = {"entur.logging.request-response.grpc.server.exception-handler.enabled"}, havingValue = "true", matchIfMissing = true)
    public RequestResponseGrpcExceptionHandlerInterceptor requestResponseGRpcExceptionHandlerInterceptor(GrpcExceptionInterceptor interceptor) {
        return new RequestResponseGrpcExceptionHandlerInterceptor(interceptor, exceptionInterceptorOrder);
    }

}