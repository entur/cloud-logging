package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import no.entur.logging.cloud.rr.grpc.filter.GrpcPayloadFilter;
import no.entur.protobuf.json.ProtobufJsonWriter;
import no.entur.protobuf.json.transcode.TranscodingProtobufJsonWriter;
import tools.jackson.core.JsonGenerator;

/**
 * Creates protobuf-to-json transcoding writers, which only use public protobuf APIs.
 */

public class TranscodingProtobufJsonWriterFactory implements ProtobufJsonWriterFactory {

    private final JsonFormat.TypeRegistry typeRegistry;

    private final int maxSize;

    /**
     * Constructor
     *
     * @param typeRegistry types for resolving google.protobuf.Any
     * @param maxSize max size of the written JSON in bytes, or -1 for no limit. Larger messages are truncated (as valid JSON).
     */

    public TranscodingProtobufJsonWriterFactory(JsonFormat.TypeRegistry typeRegistry, int maxSize) {
        this.typeRegistry = typeRegistry;
        this.maxSize = maxSize;
    }

    @Override
    public ProtobufJsonWriter<MessageOrBuilder, JsonGenerator> create(GrpcPayloadFilter filter) {
        TranscodingProtobufJsonWriter.Builder builder = TranscodingProtobufJsonWriter.newBuilder()
                .withTypeRegistry(typeRegistry)
                .withMaxSize(maxSize);

        return filter.configure(builder).build();
    }
}
