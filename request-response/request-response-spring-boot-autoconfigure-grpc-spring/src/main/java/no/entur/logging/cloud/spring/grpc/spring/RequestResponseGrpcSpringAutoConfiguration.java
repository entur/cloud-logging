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
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
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
@AutoConfigureBefore(GrpcServerAutoConfiguration.class)
public class RequestResponseGrpcSpringAutoConfiguration {

    @Value("${entur.logging.request-response.grpc.server.interceptor-order:300}")
    private int serverInterceptorOrder;

    // on-demand logging discards most log statements, so only map bodies to JSON for those which are written
    @Value("${entur.logging.grpc.ondemand.enabled:false}")
    private boolean deferredBodyMapping;

    // a deferred body is by default mapped before the log statement is handed over to the async appender;
    // this leaves the mapping to the async appender's thread instead
    @Value("${entur.logging.request-response.grpc.map-body-on-async-appender-thread:false}")
    private boolean mapBodyOnAsyncAppenderThread;

    @Bean
    @ConditionalOnMissingBean(OrderedGrpcLoggingServerInterceptor.class)
    public OrderedGrpcLoggingServerInterceptor orderedGrpcLoggingServerInterceptor(GrpcPayloadJsonMapper grpcPayloadJsonMapper, GrpcMetadataJsonMapper grpcMetadataJsonMapper, GrpcSink grpcSink, GrpcServerLoggingFilters grpcServerLoggingFilters) {
        return new OrderedGrpcLoggingServerInterceptor(grpcSink, grpcServerLoggingFilters, grpcMetadataJsonMapper, grpcPayloadJsonMapper, deferredBodyMapping, mapBodyOnAsyncAppenderThread, serverInterceptorOrder);
    }

    @Bean
    @ConditionalOnProperty(name = {"entur.logging.request-response.grpc.server.exception-handler.enabled"}, havingValue = "true", matchIfMissing = true)
    public RuntimeExceptionExceptionHandler runtimeExceptionExceptionHandler() {
        // catch-all so run late
        return new RuntimeExceptionExceptionHandler(Integer.MAX_VALUE / 2);
    }

}