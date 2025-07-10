package no.entur.logging.cloud.gcp.trace.spring.grpc;

import no.entur.logging.cloud.gcp.trace.spring.grpc.interceptor.OrderedTraceIdGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.trace.spring.grpc.GrpcCorrelationIdAutoConfiguration;
import no.entur.logging.cloud.trace.spring.grpc.interceptor.OrderedCorrelationIdGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.trace.spring.grpc.properties.GrpcMdcProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(GrpcCorrelationIdAutoConfiguration.class)
public class GcpGrpcTraceAutoConfiguration {

    @Bean
    @ConditionalOnMissingClass("io.opentelemetry.api.OpenTelemetry")
    @ConditionalOnBean(OrderedCorrelationIdGrpcMdcContextServerInterceptor.class)
    @ConditionalOnMissingBean(OrderedTraceIdGrpcMdcContextServerInterceptor.class)
    public OrderedTraceIdGrpcMdcContextServerInterceptor orderedTraceIdGrpcMdcContextServerInterceptor(OrderedCorrelationIdGrpcMdcContextServerInterceptor interceptor) {
        int order = interceptor.getOrder();

        return new OrderedTraceIdGrpcMdcContextServerInterceptor(order + 1);
    }

}
