package no.entur.logging.cloud.gcp.spring.grpc.lognet;


import com.google.protobuf.util.JsonFormat;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import no.entur.logging.cloud.gcp.micrometer.StackdriverLogbackMetrics;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcAddMdcTraceToResponseInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcTraceMdcContextInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.List;

@Configuration
public class LognetLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GrpcTraceMdcContextInterceptor.class)
    public GrpcTraceMdcContextInterceptor grpcTraceMdcContextInterceptor() {
        return GrpcTraceMdcContextInterceptor.newBuilder().build();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcMdcContextInterceptor.class)
    public GrpcMdcContextInterceptor grpcMdcContextInterceptor() {
        return GrpcMdcContextInterceptor.newBuilder().build();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcAddMdcTraceToResponseInterceptor.class)
    public GrpcAddMdcTraceToResponseInterceptor grpcAddMdcTraceToResponseInterceptor() {
        return new GrpcAddMdcTraceToResponseInterceptor();
    }

}
