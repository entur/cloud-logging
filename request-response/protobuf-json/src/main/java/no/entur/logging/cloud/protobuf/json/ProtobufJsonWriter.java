package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes protobuf messages as JSON, using the <a href="https://protobuf.dev/programming-guides/json/">ProtoJSON format</a>,
 * i.e. like {@linkplain JsonFormat#printer()} with insignificant whitespace omitted.
 * <br><br>
 * Implementations:
 * <ul>
 *     <li>{@linkplain no.entur.logging.cloud.protobuf.json.jsonformat.JsonFormatProtobufJsonWriter} - JsonFormat (default)</li>
 *     <li>{@linkplain no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter} - transcodes the binary format to JSON</li>
 *     <li>{@linkplain no.entur.logging.cloud.protobuf.json.codedoutputstream.CodedOutputStreamProtobufJsonWriter} - lets the generated
 *     serialization code write JSON via a CodedOutputStream (Java 24+)</li>
 * </ul>
 * Use {@linkplain JsonFormatComparison} to check that the output is the same as JsonFormat for your messages.
 * <br><br>
 * Implementations are thread-safe.
 */

public interface ProtobufJsonWriter {

    /**
     * Write a message as a JSON value.
     *
     * @param message message or builder
     * @param generator target
     * @throws com.google.protobuf.InvalidProtocolBufferException if a google.protobuf.Any type cannot be resolved
     * @throws IOException if writing fails
     */

    void write(MessageOrBuilder message, JsonGenerator generator) throws IOException;

    /**
     * Write a message as a JSON string.
     *
     * @param message message or builder
     * @return JSON
     * @throws com.google.protobuf.InvalidProtocolBufferException if a google.protobuf.Any type cannot be resolved
     * @throws IOException if writing fails
     */

    default String writeAsString(MessageOrBuilder message) throws IOException {
        int size = message instanceof MessageLite messageLite ? messageLite.getSerializedSize() * 2 : 256;
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(size, 32));
        try (JsonGenerator generator = ProtobufJsonWriterSupport.JSON_FACTORY.createGenerator(ObjectWriteContext.empty(), output)) {
            write(message, generator);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
