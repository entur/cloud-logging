package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import no.entur.logging.cloud.rr.grpc.filter.GrpcBodyFilter;
import no.entur.logging.cloud.rr.grpc.filter.GrpcPayloadFilter;
import no.entur.protobuf.json.ProtobufJsonWriter;
import no.entur.protobuf.json.filter.JsonFilter;
import no.entur.protobuf.json.plan.FilteredJsonValues;
import no.entur.protobuf.json.plan.ProtobufJsonWriterSupport;
import no.entur.protobuf.json.transcode.FilteringTranscodingProtobufJsonWriter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map gRPC messages to JSON using protobuf-to-json, with a writer per payload filter.
 * <br><br>
 * Messages larger than the max size of the writers are truncated (as valid JSON) rather than omitted; they are
 * only omitted if not even the first field fits. Truncation is reported for the (default) transcoding writer.
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
        return map(m, GrpcPayloadFilter.NONE, filter).getJson();
    }

    @Override
    public GrpcPayloadJson map(MessageOrBuilder m, GrpcPayloadFilter payloadFilter, GrpcBodyFilter bodyFilter) throws InvalidProtocolBufferException {
        if (m == null) {
            // i.e. empty. This kicks in when throwing StatusRuntimeExceptions
            return GrpcPayloadJson.NONE;
        }

        GrpcPayloadJson written;
        try {
            written = write(getWriter(payloadFilter), m);
        } catch (InvalidProtocolBufferException e) {
            throw e;
        } catch (IOException e) {
            // Unexpected IOException.
            throw new IllegalStateException(e);
        }

        CharSequence filtered = bodyFilter.filterBody((CharSequence) written.getJson());
        if (filtered == null) {
            return new GrpcPayloadJson(getUnableToFormatMessage(), false);
        }
        if (filtered.length() <= 2) {
            // i.e. "{}"
            if (!m.getAllFields().isEmpty()) {
                // the first field did not fit within the max size
                return new GrpcPayloadJson(getOmittedMessage(m), true);
            }
            return GrpcPayloadJson.NONE;
        }
        return new GrpcPayloadJson(filtered.toString(), written.isTruncated());
    }

    /**
     * Write a message as JSON. Whether the message was truncated is only known for the transcoding writer;
     * for other writers it is reported as not truncated.
     *
     * @param writer writer
     * @param m      message
     * @return the JSON, and whether it was truncated because of max size
     * @throws IOException if the message could not be written
     */

    protected GrpcPayloadJson write(ProtobufJsonWriter<MessageOrBuilder, JsonGenerator> writer, MessageOrBuilder m) throws IOException {
        if (!(writer instanceof FilteringTranscodingProtobufJsonWriter)) {
            return new GrpcPayloadJson(writer.writeAsString(m), false);
        }
        FilteringTranscodingProtobufJsonWriter filteringWriter = (FilteringTranscodingProtobufJsonWriter) writer;
        JsonFilter filter = filteringWriter.getFilter();

        Message message = m instanceof Message ? (Message) m : ((Message.Builder) m).buildPartial();

        long size = Math.min(message.getSerializedSize() * 2L, filter.getMaxSize());
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.max(size, 32));

        FilteredJsonValues values;
        try (JsonGenerator generator = ProtobufJsonWriterSupport.JSON_FACTORY.createGenerator(ObjectWriteContext.empty(), output)) {
            values = new FilteredJsonValues(filter, generator);
            filteringWriter.write(message, values);
        }
        return new GrpcPayloadJson(output.toString(StandardCharsets.UTF_8), values.getContext().isStopped());
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
