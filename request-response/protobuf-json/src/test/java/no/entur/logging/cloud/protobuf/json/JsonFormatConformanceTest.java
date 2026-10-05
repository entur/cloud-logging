package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf_test_messages.proto2.TestMessagesProto2.TestAllTypesProto2;
import com.google.protobuf_test_messages.proto3.TestMessagesProto3.TestAllTypesProto3;
import no.entur.logging.cloud.protobuf.json.codedoutputstream.CodedOutputStreamProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.jsonformat.JsonFormatProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Compare the output of {@linkplain ProtobufJsonWriter} with {@linkplain JsonFormat} for random messages
 * of the protobuf conformance test types.
 */

public class JsonFormatConformanceTest {

    private static final int ITERATIONS = 2000;

    // fail on duplicate keys, they are a problem in GCP
    static final JsonMapper MAPPER = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    static final JsonFormat.TypeRegistry TYPE_REGISTRY = RandomMessages.createTypeRegistry();

    static final ProtobufJsonWriter CODED_OUTPUT_STREAM_WRITER = CodedOutputStreamProtobufJsonWriter.newBuilder().withTypeRegistry(TYPE_REGISTRY).build();
    static final ProtobufJsonWriter TRANSCODING_WRITER = TranscodingProtobufJsonWriter.newBuilder().withTypeRegistry(TYPE_REGISTRY).build();

    static final JsonFormatComparison CODED_OUTPUT_STREAM = JsonFormatComparison.newBuilder().withTypeRegistry(TYPE_REGISTRY).withWriter(CODED_OUTPUT_STREAM_WRITER).build();
    static final JsonFormatComparison TRANSCODING = JsonFormatComparison.newBuilder().withTypeRegistry(TYPE_REGISTRY).withWriter(TRANSCODING_WRITER).build();
    static final JsonFormatComparison JSON_FORMAT = JsonFormatComparison.newBuilder().withTypeRegistry(TYPE_REGISTRY).withWriter(JsonFormatProtobufJsonWriter.create(TYPE_REGISTRY)).build();

    @Test
    public void codedOutputStreamIsAvailable() {
        // tests run on Java 25, with the Java 24 classes
        assertThat(CodedOutputStreamProtobufJsonWriter.getUnavailableCause()).isNull();
        assertThat(CodedOutputStreamProtobufJsonWriter.isAvailable()).isTrue();
    }

    @Test
    public void proto3() {
        compareRandomMessages(TestAllTypesProto3::newBuilder);
    }

    @Test
    public void proto2() {
        compareRandomMessages(TestAllTypesProto2::newBuilder);
    }

    private void compareRandomMessages(Supplier<Message.Builder> builders) {
        for (int seed = 0; seed < ITERATIONS; seed++) {
            Message message = new RandomMessages(seed).fill(builders.get());

            assertSameAsJsonFormat(CODED_OUTPUT_STREAM, message, "seed " + seed + " (CodedOutputStream)");
            assertSameAsJsonFormat(TRANSCODING, message, "seed " + seed + " (transcoding)");
            assertSameAsJsonFormat(JSON_FORMAT, message, "seed " + seed + " (JsonFormat)");
        }
    }

    /**
     * Compare a message, also as dynamic message and builder.
     */

    static void assertSameAsJsonFormat(JsonFormatComparison comparison, MessageOrBuilder message, String description) {
        assertSame(comparison, message, description);
        if (message instanceof Message m && !(m instanceof DynamicMessage)) {
            try {
                assertSame(comparison, DynamicMessage.parseFrom(m.getDescriptorForType(), m.toByteString()), description + " (DynamicMessage)");
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalStateException(e);
            }
            assertSame(comparison, m.toBuilder(), description + " (builder)");
        }
    }

    private static void assertSame(JsonFormatComparison comparison, MessageOrBuilder message, String description) {
        JsonFormatComparison.Result result = comparison.compare(message);
        if (!result.isSame()) {
            fail(description + ": " + result);
        }
    }

    static String write(ProtobufJsonWriter writer, MessageOrBuilder message) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = MAPPER.createGenerator(output)) {
            writer.write(message, generator);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
