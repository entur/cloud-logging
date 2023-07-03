package org.entur.logging.grpc.slf4jv17;

import com.google.protobuf.util.JsonFormat;
import io.grpc.Metadata;
import org.entur.logging.grpc.AbstractGrpcServerLoggingInterceptor;
import org.entur.logging.grpc.DefaultMetadataJsonMapper;
import org.entur.logging.grpc.GrpcStatusDetailsMapper;
import org.entur.logging.grpc.JsonPrinterFactory;
import org.entur.logging.grpc.MetadataJsonMapper;
import org.entur.logging.grpc.TypeRegistryFactory;
import org.entur.logging.grpc.filter.GrpcServerLoggingFilters;
import org.entur.logging.grpc.marker.DefaultGrpcMarkerFactory;
import org.entur.logging.grpc.marker.GrpcMarkerFactory;
import org.entur.logging.grpc.status.GrpcStatusMapper;
import org.entur.logging.grpc.status.JsonPrinterStatusMapper;
import org.entur.logging.grpc.status.LegacyGrpcStatusMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// for use with slf4j 1.7.x series
public class ClassicGrpcServerLoggingInterceptor extends AbstractGrpcServerLoggingInterceptor {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        public static final int DEFAULT_JSON_MESSAGE_SIZE = 99 * 1024;
        public static final int DEFAULT_BINARY_MESSAGE_SIZE = 40 * 1024;

        private boolean prettyPrint = false;
        private int maxJsonMessageLength = -1;
        private int maxBinaryMessageLength = -1;
        private GrpcStatusDetailsMapper mapper;
        private GrpcServerLoggingFilters filters = null;
        private Map<String, Function<Metadata, Object>> keyMappers = new HashMap<>();

        private JsonFormat.TypeRegistry typeRegistry;

        public Builder withPrettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        public Builder withKeyMapper(String key, Function<Metadata, Object> mapper) {
            this.keyMappers.put(key, mapper);
            return this;
        }

        public Builder withKeyMappers(Map<String, Function<Metadata, Object>> keyMappers) {
            this.keyMappers = keyMappers;
            return this;
        }

        /**
         * @deprecated use TypeRegistry specified with status details types instead
         * @param mapper to be used
         * @return this builder instance
         */
        public Builder withStatusDetailsMapper(GrpcStatusDetailsMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder withTypeRegistry(JsonFormat.TypeRegistry typeRegistry) {
            this.typeRegistry = typeRegistry;
            return this;
        }

        public Builder withMaxJsonMessageLength(int maxJsonMessageLength) {
            this.maxJsonMessageLength = maxJsonMessageLength;
            return this;
        }

        public Builder withMaxBinaryMessageLength(int maxBinaryMessageLength) {
            this.maxBinaryMessageLength = maxBinaryMessageLength;
            return this;
        }

        public Builder withFilters(GrpcServerLoggingFilters filters) {
            this.filters = filters;
            return this;
        }

        public ClassicGrpcServerLoggingInterceptor build() {
            if (maxJsonMessageLength == -1) {
                maxJsonMessageLength = DEFAULT_JSON_MESSAGE_SIZE;
            }

            if (maxBinaryMessageLength == -1) {
                maxBinaryMessageLength = DEFAULT_BINARY_MESSAGE_SIZE;
            }

            if (typeRegistry == null) {
                typeRegistry = TypeRegistryFactory.createDefaultTypeRegistry();
            }

            JsonFormat.Printer printer = JsonPrinterFactory.createPrinter(prettyPrint, typeRegistry);

            DefaultGrpcMarkerFactory factory = new DefaultGrpcMarkerFactory(printer, maxJsonMessageLength, maxBinaryMessageLength);

            if (filters == null) {
                filters = GrpcServerLoggingFilters.classic();
            }

            GrpcStatusMapper grpcStatusMapper;
            if (mapper != null) {
                // Use deprecated grpc status detail mapping for logging grpc status if specifed by client
                grpcStatusMapper = new LegacyGrpcStatusMapper(mapper);
            } else {
                grpcStatusMapper = new JsonPrinterStatusMapper(printer);
            }

            return new ClassicGrpcServerLoggingInterceptor(factory, filters, new DefaultMetadataJsonMapper(grpcStatusMapper, keyMappers));
        }
    }

    public ClassicGrpcServerLoggingInterceptor(GrpcMarkerFactory grpcMarkerFactory, GrpcServerLoggingFilters filters, MetadataJsonMapper mapper) {
        super(grpcMarkerFactory, filters, mapper);
    }

    public void postLogStatement() {
        // handle MDC, omitted
    }

    public void preLogStatement() {
        // handle MDC, omitted
    }

}
