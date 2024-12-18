package no.entur.logging.cloud.spring.grpc.ecosystem;

import com.google.protobuf.util.JsonFormat;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration;
import net.devh.boot.grpc.server.error.GrpcExceptionInterceptor;
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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;

@Configuration
@AutoConfigureAfter(GrpcAdviceAutoConfiguration.class)
public class RequestResponseGrpcEcosystemAutoConfiguration extends AbstractRequestResponseGrpcEcosystemSinkAutoConfiguration {

    @Value("${entur.logging.request-response.max-size}")
    protected int maxSize;

    @Value("${entur.logging.request-response.max-body-size}")
    protected int maxBodySize;

    @Value("${entur.logging.request-response.grpc.server.interceptor-order:0}")
    private int serverInterceptorOrder;

    @Value("${entur.logging.request-response.grpc.client.interceptor-order:0}")
    private int clientInterceptorOrder;

    @Value("${entur.logging.request-response.grpc.server.exception-handler.interceptor-order:0}")
    private int exceptionInterceptorOrder;

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
    @ConditionalOnMissingBean(OrderedGrpcLoggingServerInterceptor.class)
    public OrderedGrpcLoggingServerInterceptor orderedGrpcLoggingServerInterceptor(GrpcPayloadJsonMapper grpcPayloadJsonMapper, GrpcMetadataJsonMapper grpcMetadataJsonMapper, GrpcSink grpcSink, GrpcServerLoggingFilters grpcServerLoggingFilters) {
        return new OrderedGrpcLoggingServerInterceptor(grpcSink, grpcServerLoggingFilters, grpcMetadataJsonMapper, grpcPayloadJsonMapper, serverInterceptorOrder);
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

    // handle status runtime exception

    @Bean
    @ConditionalOnProperty(name = {"entur.logging.request-response.grpc.server.exception-handler.enabled"}, havingValue = "true", matchIfMissing = true)
    @Primary
    public RequestResponseGrpcExceptionHandlerInterceptor requestResponseGRpcExceptionHandlerInterceptor(GrpcExceptionInterceptor interceptor) {
        return new RequestResponseGrpcExceptionHandlerInterceptor(interceptor, exceptionInterceptorOrder);
    }

}
