package no.entur.logging.cloud.spring.rr.grpc;

import com.google.protobuf.util.JsonFormat;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class RequestResponseGrpcAutoConfiguration extends AbstractRequestResponseGrpcSinkAutoConfiguration {

    @Value("${entur.logging.request-response.max-size:-1}")
    private int maxSize;

    @Value("${entur.logging.request-response.max-body-size:-1}")
    private int maxBodySize;

    @Autowired
    protected GrpcLoggingCloudProperties grpcLoggingCloudProperties;

    @Value("${entur.logging.request-response.grpc.client.interceptor-order:0}")
    private int clientInterceptorOrder;

    protected int getMaxBodySize() {
        if(maxBodySize == -1) {
            return grpcLoggingCloudProperties.getMaxBodySize();
        }
        return Math.min(grpcLoggingCloudProperties.getMaxBodySize(), maxBodySize);
    }

    protected int getMaxSize() {
        if(maxSize == -1) {
            return grpcLoggingCloudProperties.getMaxSize();
        }
        return Math.min(grpcLoggingCloudProperties.getMaxSize(), maxSize);
    }

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
        int max = getMaxBodySize();
        return new DefaultGrpcPayloadJsonMapper(printer, max, max / 2);
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
    @ConditionalOnMissingBean(OrderedGrpcLoggingClientInterceptor.class)
    public OrderedGrpcLoggingClientInterceptor orderedGrpcLoggingClientInterceptor(GrpcPayloadJsonMapper grpcPayloadJsonMapper, GrpcMetadataJsonMapper grpcMetadataJsonMapper, GrpcSink grpcSink, GrpcClientLoggingFilters grpcServiceLoggingFilters) {
        return new OrderedGrpcLoggingClientInterceptor(grpcSink, grpcServiceLoggingFilters, grpcMetadataJsonMapper, grpcPayloadJsonMapper, clientInterceptorOrder);
    }

    @Bean
    @ConditionalOnMissingBean(GrpcSink.class)
    public GrpcSink grpcSink() {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = parseLevel(loggerLevel);

        return createMachineReadbleSink(logger, level);
    }

}
