package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import no.entur.logging.cloud.rr.grpc.filter.GrpcBodyFilter;
import no.entur.logging.cloud.rr.grpc.filter.GrpcPayloadFilter;
import no.entur.protobuf.json.ProtobufJsonWriter;
import tools.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map gRPC messages to JSON using protobuf-to-json, with a writer per payload filter.
 * <br><br>
 * Messages larger than the max size of the writers are truncated (as valid JSON) rather than omitted; they are
 * only omitted if not even the first field fits.
 */

public class ProtobufJsonWriterGrpcPayloadJsonMapper implements GrpcPayloadJsonMapper {

    private final ProtobufJsonWriterFactory factory;

    private final ProtobufJsonWriter<MessageOrBuilder, JsonGenerator> defaultWriter;

    // payload filters are configured up front, so this map stays small
    private final Map<GrpcPayloadFilter, ProtobufJsonWriter<MessageOrBuilder, JsonGenerator>> writers = new ConcurrentHashMap<>();

    public ProtobufJsonWriterGrpcPayloadJsonMapper(ProtobufJsonWriterFactory factory) {
        this.factory = factory;
        this.defaultWriter = factory.create(GrpcPayloadFilter.NONE);
    }

    @Override
    public String map(MessageOrBuilder m, GrpcBodyFilter filter) throws InvalidProtocolBufferException {
        return map(m, GrpcPayloadFilter.NONE, filter);
    }

    @Override
    public String map(MessageOrBuilder m, GrpcPayloadFilter payloadFilter, GrpcBodyFilter bodyFilter) throws InvalidProtocolBufferException {
        if (m == null) {
            // i.e. empty. This kicks in when throwing StatusRuntimeExceptions
            return null;
        }

        String json;
        try {
            json = getWriter(payloadFilter).writeAsString(m);
        } catch (InvalidProtocolBufferException e) {
            throw e;
        } catch (IOException e) {
            // Unexpected IOException.
            throw new IllegalStateException(e);
        }

        CharSequence filtered = bodyFilter.filterBody((CharSequence) json);
        if (filtered == null) {
            return getUnableToFormatMessage();
        }
        if (filtered.length() <= 2) {
            // i.e. "{}"
            if (!m.getAllFields().isEmpty()) {
                // the first field did not fit within the max size
                return getOmittedMessage(m);
            }
            return null;
        }
        return filtered.toString();
    }

    protected ProtobufJsonWriter<MessageOrBuilder, JsonGenerator> getWriter(GrpcPayloadFilter payloadFilter) {
        if (payloadFilter.isNone()) {
            return defaultWriter;
        }
        return writers.computeIfAbsent(payloadFilter, factory::create);
    }

    protected String getUnableToFormatMessage() {
        return "\"Unable to format message\"";
    }

    /**
     * Message for when not even the first field fits within the max size.
     *
     * @param m message
     * @return message; as raw JSON
     */

    protected String getOmittedMessage(MessageOrBuilder m) {
        if (m instanceof MessageLite) {
            return "\"Omitted binary message size " + ((MessageLite) m).getSerializedSize() + "\"";
        }
        return "\"Omitted message\"";
    }

}
