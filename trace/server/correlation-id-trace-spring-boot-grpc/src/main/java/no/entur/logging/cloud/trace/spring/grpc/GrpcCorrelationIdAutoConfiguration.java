package no.entur.logging.cloud.trace.spring.grpc;

import no.entur.logging.cloud.grpc.mdc.InitializeGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.grpc.trace.CorrelationIdRequiredServerInterceptor;
import no.entur.logging.cloud.trace.spring.grpc.interceptor.OrderedCorrelationIdGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.trace.spring.grpc.properties.GrpcMdcProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {GrpcMdcProperties.class})
@ConditionalOnProperty(name = {"entur.logging.grpc.trace.mdc.enabled"}, havingValue = "true", matchIfMissing = true)
public class GrpcCorrelationIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OrderedCorrelationIdGrpcMdcContextServerInterceptor.class)
    public OrderedCorrelationIdGrpcMdcContextServerInterceptor orderedCorrelationIdGrpcMdcContextServerInterceptor(GrpcMdcProperties properties) {
        return new OrderedCorrelationIdGrpcMdcContextServerInterceptor(properties.isRequired(), properties.isResponse(), new CorrelationIdRequiredServerInterceptor.DefaultCorrelationIdListener(), properties.getInterceptorOrder());
    }

}
