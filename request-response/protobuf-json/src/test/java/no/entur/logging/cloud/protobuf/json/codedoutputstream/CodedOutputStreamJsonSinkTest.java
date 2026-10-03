package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.WireFormat;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf_test_messages.proto3.TestMessagesProto3.TestAllTypesProto3;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan.Plans;
import no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;
import java.lang.reflect.Proxy;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class CodedOutputStreamJsonSinkTest {

    private static final Descriptor DESCRIPTOR = TestAllTypesProto3.getDescriptor();
    private static final int MAP_STRING_STRING = DESCRIPTOR.findFieldByName("map_string_string").getNumber();

    @Test
    public void skipsDuplicateMapKeys() throws Exception {
        // generated messages never have duplicate map keys, so write the entries directly
        assumeTrue(CodedOutputStreamProtobufJsonWriter.isAvailable());

        Message message = message(output -> {
            // map entry as tag, length, key and value
            writeTaggedEntry(output, "a", "1");
            writeTaggedEntry(output, "b", "2");
            writeTaggedEntry(output, "a", "3");
            // map entry as a message
            output.writeMessage(MAP_STRING_STRING, entry("b", "4"));
            output.writeMessage(MAP_STRING_STRING, entry("c", "5"));
        });

        assertThat(write(message)).isEqualTo("{\"mapStringString\":{\"a\":\"1\",\"b\":\"2\",\"c\":\"5\"}}");
    }

    private interface Writer {
        void writeTo(CodedOutputStream output) throws Exception;
    }

    private static Message message(Writer writer) {
        return (Message) Proxy.newProxyInstance(CodedOutputStreamJsonSinkTest.class.getClassLoader(), new Class<?>[]{Message.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getDescriptorForType":
                    return DESCRIPTOR;
                case "writeTo":
                    writer.writeTo((CodedOutputStream) args[0]);
                    return null;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
    }

    private static void writeTaggedEntry(CodedOutputStream output, String key, String value) throws Exception {
        output.writeTag(MAP_STRING_STRING, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        output.writeUInt32NoTag(CodedOutputStream.computeStringSize(1, key) + CodedOutputStream.computeStringSize(2, value));
        output.writeString(1, key);
        output.writeString(2, value);
    }

    private static DynamicMessage entry(String key, String value) {
        Descriptor type = DESCRIPTOR.findFieldByName("map_string_string").getMessageType();
        return DynamicMessage.newBuilder(type)
                .setField(type.findFieldByName("key"), key)
                .setField(type.findFieldByName("value"), value)
                .build();
    }

    private static String write(Message message) throws Exception {
        Plans plans = new Plans();
        TranscodingProtobufJsonWriter transcoder = new TranscodingProtobufJsonWriter(plans, JsonFormat.TypeRegistry.getEmptyTypeRegistry());
        StringWriter output = new StringWriter();
        try (JsonGenerator generator = JsonMapper.builder().build().createGenerator(output)) {
            new CodedOutputStreamJsonSink(plans, transcoder, generator).write(message);
        }
        return output.toString();
    }
}
