package org.entur.logging.grpc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.grpc.Metadata;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import org.entur.logging.grpc.status.GrpcStatusMapper;
import org.entur.logging.grpc.status.LegacyGrpcStatusMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultMetadataJsonMapper implements MetadataJsonMapper {

    // from StatusProto
    protected static final Metadata.Key<com.google.rpc.Status> STATUS_DETAILS_KEY =
            Metadata.Key.of(
                    "grpc-status-details-bin",
                    ProtoLiteUtils.metadataMarshaller(com.google.rpc.Status.getDefaultInstance()));

    public static Map<String, Metadata.Key<String>> getDefaultAsciiKeys() {
        List<String> keys = Arrays.asList(
                "x-forwarded-for",
                "accept",
                "x-envoy-original-method",
                "x-request-id",
                "x-cloud-trace-context",
                "x-forwarded-server",
                "x-forwarded-host",
                "x-real-ip",
                "x-forwarded-proto",
                "traceparent",
                "accept-encoding",
                "x-endpoint-api-consumer-number",
                "x-api-key",
                "x-forwarded-port",
                "x-correlation-id",
                "content-type",
                "via",
                "x-endpoint-api-consumer-type",
                "user-agent",
                "x-envoy-original-path");

        return keys.stream().collect(Collectors.toMap(Function.identity(), key -> Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)));
    }

    @JsonSerialize(using = RawSerializer.class)
    public static class RawString {
        private final String string;

        public RawString(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }
    }

    public static class RawSerializer extends StdSerializer<RawString> {

        protected RawSerializer() {
            super(RawString.class);
        }

        @Override
        public void serialize(RawString value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeRaw(value.getString());
        }
    }

    protected final GrpcStatusMapper statusMapper;
    protected final Map<String, Function<Metadata, Object>> keyMappers;

    // optimization: cache ascii keys
    protected final ConcurrentHashMap<String, Metadata.Key<String>> asciiKeys;

    /**
     * @deprecated use constructor with statusMapper instead
     */
    @Deprecated
    public DefaultMetadataJsonMapper(GrpcStatusDetailsMapper statusDetailsMapper, Map<String, Function<Metadata, Object>> keyMappers) {
        this(statusDetailsMapper, keyMappers, getDefaultAsciiKeys());
    }

    /**
     * @deprecated use constructor with statusMapper instead
     */
    @Deprecated
    public DefaultMetadataJsonMapper(GrpcStatusDetailsMapper statusDetailsMapper, Map<String, Function<Metadata, Object>> keyMappers, Map<String, Metadata.Key<String>> asciiHeaders) {
        this.statusMapper = new LegacyGrpcStatusMapper(statusDetailsMapper);
        this.keyMappers = keyMappers;
        this.asciiKeys = new ConcurrentHashMap<>(asciiHeaders);
    }

    public DefaultMetadataJsonMapper(GrpcStatusMapper statusMapper, Map<String, Function<Metadata, Object>> keyMappers) {
        this(statusMapper, keyMappers, getDefaultAsciiKeys());
    }


    public DefaultMetadataJsonMapper(GrpcStatusMapper statusMapper, Map<String, Function<Metadata, Object>> keyMappers, Map<String, Metadata.Key<String>> asciiHeaders) {
        this.statusMapper = statusMapper;
        this.keyMappers = keyMappers;
        this.asciiKeys = new ConcurrentHashMap<>(asciiHeaders);
    }

    @Override
    public Map<String, Object> toJson(Metadata headers) {

        // this iterator is kind of inefficient, but this is all that is offered by the Metadata class
        Set<String> keys = headers.keys();
        if (keys.isEmpty()) {
            return null;
        }

        Map<String, Object> map = new HashMap<>(keys.size() * 2);
        for (String key : keys) {
            Function<Metadata, Object> f = keyMappers.get(key);
            if (f != null) {
                Object apply = f.apply(headers);
                if (apply != null) {
                    map.put(key, apply);
                }
            } else if (!key.endsWith("-bin")) {
                Metadata.Key<String> of = getAsciiKey(key);
                map.put(key, headers.get(of));
            } else if (key.equals("grpc-status-details-bin")) {
                com.google.rpc.Status statusProto = headers.get(STATUS_DETAILS_KEY);
                map.put("grpc-status-details", statusMapper.map(statusProto));
            }
        }
        return map;
    }

    protected Metadata.Key<String> getAsciiKey(String key) {
        Metadata.Key<String> of = asciiKeys.get(key);
        if (of == null) {
            // should not happen often and only for custom headers
            of = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            asciiKeys.put(key, of);
        }
        return of;
    }


}
