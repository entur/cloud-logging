package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Duration;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import com.google.protobuf.WireFormat;
import com.google.protobuf_test_messages.proto2.TestMessagesProto2.TestAllTypesProto2;
import com.google.protobuf_test_messages.proto3.TestMessagesProto3.TestAllTypesProto3;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static no.entur.logging.cloud.protobuf.json.JsonFormatConformanceTest.TYPE_REGISTRY;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProtobufJsonWriterTest {

    private final List<ProtobufJsonWriter> writers = List.of(
            JsonFormatConformanceTest.CODED_OUTPUT_STREAM_WRITER,
            JsonFormatConformanceTest.TRANSCODING_WRITER
    );

    private static int number(String name) {
        return TestAllTypesProto2.getDescriptor().findFieldByName(name).getNumber();
    }

    private void assertSameAsJsonFormat(MessageOrBuilder message) {
        JsonFormatConformanceTest.assertSameAsJsonFormat(JsonFormatConformanceTest.CODED_OUTPUT_STREAM, message, "CodedOutputStream");
        JsonFormatConformanceTest.assertSameAsJsonFormat(JsonFormatConformanceTest.TRANSCODING, message, "transcoding");
    }

    private List<String> write(MessageOrBuilder message) throws Exception {
        return writers.stream().map(w -> {
            try {
                return JsonFormatConformanceTest.write(w, message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    @Test
    public void skipsUnknownFields() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(bytes);
        output.writeInt32(number("optional_int32"), 1);
        output.writeUInt64(9999, 1);
        output.writeString(9998, "unknown");
        output.writeTag(9997, WireFormat.WIRETYPE_START_GROUP);
        output.writeInt32(1, 1);
        output.writeTag(9997, WireFormat.WIRETYPE_END_GROUP);
        output.writeFixed32(9996, 1);
        output.writeFixed64(9995, 1);
        output.flush();

        TestAllTypesProto2 message = TestAllTypesProto2.parseFrom(bytes.toByteArray());
        assertThat(message.getUnknownFields().asMap()).hasSize(5);

        assertSameAsJsonFormat(message);
        assertThat(write(message)).containsExactly("{\"optionalInt32\":1}", "{\"optionalInt32\":1}");
    }

    @Test
    public void skipsUnknownClosedEnumValues() throws Exception {
        // protobuf keeps unknown values of closed (proto2) enums as unknown fields,
        // they are then serialized after the known fields - possibly with a higher field number than the last known field

        for (boolean withKnownFields : new boolean[]{true, false}) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            CodedOutputStream output = CodedOutputStream.newInstance(bytes);
            output.writeInt32(number("optional_int32"), 1);
            output.writeEnum(number("optional_nested_enum"), 99);

            int repeated = number("repeated_nested_enum");
            output.writeEnum(repeated, withKnownFields ? 1 : 77);
            output.writeEnum(repeated, 77);
            output.writeEnum(repeated, withKnownFields ? 2 : 78);

            int packed = number("packed_nested_enum");
            output.writeTag(packed, WireFormat.WIRETYPE_LENGTH_DELIMITED);
            int first = withKnownFields ? 1 : 88;
            output.writeUInt32NoTag(CodedOutputStream.computeEnumSizeNoTag(first) + CodedOutputStream.computeEnumSizeNoTag(88));
            output.writeEnumNoTag(first);
            output.writeEnumNoTag(88);

            int map = number("map_string_nested_enum");
            for (int value : withKnownFields ? new int[]{99, 1} : new int[]{99}) {
                ByteArrayOutputStream entryBytes = new ByteArrayOutputStream();
                CodedOutputStream entry = CodedOutputStream.newInstance(entryBytes);
                entry.writeString(1, "key" + value);
                entry.writeEnum(2, value);
                entry.flush();
                output.writeBytes(map, ByteString.copyFrom(entryBytes.toByteArray()));
            }
            output.flush();

            TestAllTypesProto2 message = TestAllTypesProto2.parseFrom(bytes.toByteArray());
            assertThat(message.getUnknownFields().asMap()).isNotEmpty();

            assertSameAsJsonFormat(message);
        }
    }

    @Test
    public void wellKnownTypesAsRoot() throws Exception {
        assertSameAsJsonFormat(Timestamp.newBuilder().setSeconds(1700000000).setNanos(123000000).build());
        assertSameAsJsonFormat(Duration.newBuilder().setSeconds(-3).setNanos(-500).build());
        assertSameAsJsonFormat(FieldMask.newBuilder().addPaths("foo_bar.baz").build());
        assertSameAsJsonFormat(Int64Value.of(Long.MIN_VALUE));
        assertSameAsJsonFormat(Struct.newBuilder().putFields("a", Value.newBuilder().setStringValue("b").build()).build());
        assertSameAsJsonFormat(Value.newBuilder().setListValue(ListValue.newBuilder().addValues(Value.newBuilder().setBoolValue(true))).build());
        assertSameAsJsonFormat(Any.pack(TestAllTypesProto3.newBuilder().setOptionalString("x").setOptionalTimestamp(Timestamp.getDefaultInstance()).build()));
        assertSameAsJsonFormat(Any.pack(Duration.newBuilder().setSeconds(1).build()));
        assertSameAsJsonFormat(Empty.getDefaultInstance());
        assertSameAsJsonFormat(TestAllTypesProto3.getDefaultInstance());
    }

    @Test
    public void builder() throws Exception {
        assertSameAsJsonFormat(TestAllTypesProto3.newBuilder().setOptionalString("x").putMapStringString("a", "b"));
    }

    @Test
    public void invalidUtf8() throws Exception {
        // proto2 strings are not validated
        TestAllTypesProto2 message = TestAllTypesProto2.newBuilder()
                .setOptionalStringBytes(ByteString.copyFrom(new byte[]{'a', (byte) 0xff, 'b'}))
                .build();

        assertSameAsJsonFormat(message);
    }

    @Test
    public void unresolvableAnyFails() {
        TestAllTypesProto3 message = TestAllTypesProto3.newBuilder()
                .setOptionalAny(Any.pack(TestAllTypesProto2.getDefaultInstance()))
                .build();

        for (ProtobufJsonWriter writer : writers) {
            InvalidProtocolBufferException e = assertThrows(InvalidProtocolBufferException.class, () -> JsonFormatConformanceTest.write(writer, message));
            assertThat(e).hasMessageThat().contains("Cannot find type for url");
        }
    }

    @Test
    public void writesOutOfRangeTimestampAsObject() throws Exception {
        // JsonFormat fails
        TestAllTypesProto3 message = TestAllTypesProto3.newBuilder()
                .setOptionalTimestamp(Timestamp.newBuilder().setSeconds(Long.MAX_VALUE).setNanos(1))
                .build();

        assertThat(write(message)).containsExactly(
                "{\"optionalTimestamp\":{\"seconds\":\"9223372036854775807\",\"nanos\":1}}",
                "{\"optionalTimestamp\":{\"seconds\":\"9223372036854775807\",\"nanos\":1}}");
    }

    @Test
    public void writesNaNValueAsString() throws Exception {
        // JsonFormat fails
        TestAllTypesProto3 message = TestAllTypesProto3.newBuilder()
                .setOptionalValue(Value.newBuilder().setNumberValue(Double.NaN))
                .build();

        assertThat(write(message)).containsExactly("{\"optionalValue\":\"NaN\"}", "{\"optionalValue\":\"NaN\"}");
    }

    @Test
    public void skipsDuplicateMapKeys() throws Exception {
        // a DynamicMessage can have map entries with the same key, generated messages cannot
        Descriptor descriptor = TestAllTypesProto3.getDescriptor();
        FieldDescriptor map = descriptor.findFieldByName("map_string_string");
        DynamicMessage message = DynamicMessage.newBuilder(descriptor)
                .addRepeatedField(map, entry(map, "a", "1"))
                .addRepeatedField(map, entry(map, "b", "2"))
                .addRepeatedField(map, entry(map, "a", "3"))
                .build();

        assertThat(write(message)).containsExactly(
                "{\"mapStringString\":{\"a\":\"1\",\"b\":\"2\"}}",
                "{\"mapStringString\":{\"a\":\"1\",\"b\":\"2\"}}");
    }

    private static DynamicMessage entry(FieldDescriptor map, String key, String value) {
        Descriptor type = map.getMessageType();
        return DynamicMessage.newBuilder(type)
                .setField(type.findFieldByName("key"), key)
                .setField(type.findFieldByName("value"), value)
                .build();
    }
}
