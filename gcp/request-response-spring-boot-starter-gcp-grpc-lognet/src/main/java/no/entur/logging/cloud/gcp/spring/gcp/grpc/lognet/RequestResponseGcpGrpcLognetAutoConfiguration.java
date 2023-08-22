package no.entur.logging.cloud.gcp.spring.gcp.grpc.lognet;

import com.google.protobuf.util.JsonFormat;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingClientInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.filter.GrpcClientLoggingFilters;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultGrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcStatusMapper;
import no.entur.logging.cloud.rr.grpc.mapper.JsonPrinterFactory;
import no.entur.logging.cloud.rr.grpc.mapper.JsonPrinterStatusMapper;
import no.entur.logging.cloud.rr.grpc.mapper.TypeRegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;

@Configuration
@PropertySource(value = "classpath:request-response.gcp.properties", ignoreResourceNotFound = false)
public class RequestResponseGcpGrpcLognetAutoConfiguration extends AbstractRequestResponseGcpGrpcLognetAutoConfiguration {

    @Value("${entur.logging.request-response.max-size}")
    protected int maxSize;

    @Value("${entur.logging.request-response.max-body-size}")
    protected int maxBodySize;

    @Bean
    @ConditionalOnMissingBean(JsonFormat.TypeRegistry.class)
    public JsonFormat.TypeRegistry jsonFormatTypeRegistry() {
        return TypeRegistryFactory.createDefaultTypeRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcStatusMapper.class)
    public GrpcStatusMapper grpcStatusMapper(JsonFormat.TypeRegistry typeRegistry) {
        JsonFormat.Printer printer = JsonPrinterFactory.createPrinter(false, typeRegistry);
        return new JsonPrinterStatusMapper(printer);
    }

    @Bean
    @ConditionalOnMissingBean(GrpcPayloadJsonMapper.class)
    public GrpcPayloadJsonMapper grpcPayloadJsonMapper(JsonFormat.TypeRegistry typeRegistry) {
        JsonFormat.Printer printer = JsonPrinterFactory.createPrinter(false, typeRegistry);
        return new DefaultGrpcPayloadJsonMapper(printer, maxBodySize, maxBodySize / 2);
    }

    @Bean
    @ConditionalOnMissingBean(GrpcMetadataJsonMapper.class)
    public GrpcMetadataJsonMapper grpcMetadataJsonMapper(GrpcStatusMapper grpcStatusMapper) {
        return new DefaultMetadataJsonMapper(grpcStatusMapper, new HashMap<>());
    }

    @Bean
    @ConditionalOnMissingBean(GrpcClientLoggingFilters.class)
    public GrpcClientLoggingFilters grpcClientLoggingFilters() {
        return GrpcClientLoggingFilters.newBuilder().classicDefaultLogging().build();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcServerLoggingFilters.class)
    public GrpcServerLoggingFilters grpcServerLoggingFilters() {
        return GrpcServerLoggingFilters.newBuilder().classicDefaultLogging().build();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcLoggingServerInterceptor.class)
    public GrpcLoggingServerInterceptor grpcLoggingServerInterceptor(GrpcPayloadJsonMapper grpcPayloadJsonMapper, GrpcMetadataJsonMapper grpcMetadataJsonMapper, GrpcSink grpcSink, GrpcServerLoggingFilters grpcServerLoggingFilters) {
        return GrpcLoggingServerInterceptor
                .newBuilder()
                .withPayloadJsonMapper(grpcPayloadJsonMapper)
                .withMetadataJsonMapper(grpcMetadataJsonMapper)
                .withSink(grpcSink)
                .withFilters(grpcServerLoggingFilters)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcLoggingClientInterceptor.class)
    public GrpcLoggingClientInterceptor grpcLoggingClientInterceptor(GrpcPayloadJsonMapper grpcPayloadJsonMapper, GrpcMetadataJsonMapper grpcMetadataJsonMapper, GrpcSink grpcSink, GrpcClientLoggingFilters grpcServiceLoggingFilters) {
        return GrpcLoggingClientInterceptor
                .newBuilder()
                .withPayloadJsonMapper(grpcPayloadJsonMapper)
                .withMetadataJsonMapper(grpcMetadataJsonMapper)
                .withSink(grpcSink)
                .withFilters(grpcServiceLoggingFilters)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(GrpcSink.class)
    public GrpcSink grpcSink() {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = parseLevel(loggerLevel);

        return createMachineReadbleSink(logger, level);
    }

}
