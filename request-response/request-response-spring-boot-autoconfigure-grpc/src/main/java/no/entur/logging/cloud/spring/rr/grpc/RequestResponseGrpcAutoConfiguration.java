package no.entur.logging.cloud.spring.rr.grpc;

import com.google.protobuf.util.JsonFormat;
import no.entur.logging.cloud.protobuf.json.ProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.codedoutputstream.CodedOutputStreamProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.jsonformat.JsonFormatProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter;
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
import java.util.Locale;

@Configuration
public class RequestResponseGrpcAutoConfiguration extends AbstractRequestResponseGrpcSinkAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseGrpcAutoConfiguration.class);

    /**
     * Create a JSON writer for gRPC message bodies.
     *
     * @param name json-format (default), transcoding or coded-output-stream
     * @param typeRegistry types for resolving google.protobuf.Any
     * @return writer
     */

    public static ProtobufJsonWriter createProtobufJsonWriter(String name, JsonFormat.TypeRegistry typeRegistry) {
        switch (name.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "")) {
            case "jsonformat": {
                return new JsonFormatProtobufJsonWriter(JsonPrinterFactory.createPrinter(false, typeRegistry));
            }
            case "transcoding": {
                return TranscodingProtobufJsonWriter.newBuilder().withTypeRegistry(typeRegistry).build();
            }
            case "codedoutputstream": {
                if (!CodedOutputStreamProtobufJsonWriter.isAvailable()) {
                    LOGGER.warn("JSON-writing CodedOutputStream for gRPC request-response logging is not available ({}), using transcoding instead", CodedOutputStreamProtobufJsonWriter.getUnavailableCause().toString());
                }
                return CodedOutputStreamProtobufJsonWriter.newBuilder().withTypeRegistry(typeRegistry).build();
            }
            default: {
                throw new IllegalStateException("Unknown gRPC request-response JSON writer '" + name + "', expected json-format, transcoding or coded-output-stream");
            }
        }
    }

    @Value("${entur.logging.request-response.max-size:-1}")
    private int maxSize;

    @Value("${entur.logging.request-response.max-body-size:-1}")
    private int maxBodySize;

    @Autowired
    protected GrpcLoggingCloudProperties grpcLoggingCloudProperties;

    @Value("${entur.logging.request-response.grpc.client.interceptor-order:0}")
    private int clientInterceptorOrder;

    @Value("${entur.logging.request-response.grpc.json-writer:json-format}")
    private String jsonWriter;

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
    @ConditionalOnMissingBean(ProtobufJsonWriter.class)
    public ProtobufJsonWriter grpcProtobufJsonWriter(JsonFormat.TypeRegistry typeRegistry) {
        return createProtobufJsonWriter(jsonWriter, typeRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(GrpcPayloadJsonMapper.class)
    public GrpcPayloadJsonMapper grpcPayloadJsonMapper(ProtobufJsonWriter protobufJsonWriter) {
        int max = getMaxBodySize();
        return new DefaultGrpcPayloadJsonMapper(protobufJsonWriter, max, max / 2);
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
