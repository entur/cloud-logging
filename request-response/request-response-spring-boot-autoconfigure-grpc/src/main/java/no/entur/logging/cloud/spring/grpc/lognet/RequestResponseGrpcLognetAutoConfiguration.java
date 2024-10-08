package no.entur.logging.cloud.spring.grpc.lognet;

import com.google.protobuf.util.JsonFormat;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
import org.lognet.springboot.grpc.FailureHandlingSupport;
import org.lognet.springboot.grpc.autoconfigure.ConditionalOnMissingErrorHandler;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.lognet.springboot.grpc.recovery.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;

@Configuration
@AutoConfigureAfter(GRpcAutoConfiguration.class)
public class RequestResponseGrpcLognetAutoConfiguration extends AbstractRequestResponseGrpcLognetSinkAutoConfiguration {

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

    @Bean
    @ConditionalOnBean({FailureHandlingSupport.class, GRpcExceptionHandlerMethodResolver.class})
    @ConditionalOnProperty(name = {"entur.logging.request-response.grpc.server.exception-handler.enabled"}, havingValue = "true", matchIfMissing = true)
    @Primary
    public RequestResponseGrpcExceptionHandlerInterceptor requestResponseGRpcExceptionHandlerInterceptor(GRpcExceptionHandlerInterceptor interceptor) {
        return new RequestResponseGrpcExceptionHandlerInterceptor(interceptor, exceptionInterceptorOrder);
    }

    /**
     *
     * Without any exception handlers, the exception handler does not handle status runtime exceptions either
     *
     */

    @ConditionalOnMissingErrorHandler(StatusRuntimeException.class)
    @Configuration
    static class DefaultStatusRuntimeExceptionErrorHandlerConfig {

        @GRpcServiceAdvice
        public static class StatusRuntimeExceptionGRpcServiceAdvice {
            @java.lang.SuppressWarnings("all")
            @GRpcExceptionHandler
            public Status handle(StatusRuntimeException e, GRpcExceptionScope scope) {
                return e.getStatus();
            }
        }
    }
}
