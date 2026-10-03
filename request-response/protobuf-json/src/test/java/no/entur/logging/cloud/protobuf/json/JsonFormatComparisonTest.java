package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.Any;
import com.google.protobuf.Value;
import com.google.protobuf_test_messages.proto2.TestMessagesProto2.TestAllTypesProto2;
import com.google.protobuf_test_messages.proto3.TestMessagesProto3.TestAllTypesProto3;
import no.entur.logging.cloud.protobuf.json.codedoutputstream.CodedOutputStreamProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static no.entur.logging.cloud.protobuf.json.JsonFormatConformanceTest.TYPE_REGISTRY;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonFormatComparisonTest {

    private final JsonFormatComparison comparison = JsonFormatComparison.newBuilder()
            .withTypeRegistry(TYPE_REGISTRY)
            .withWriter(TranscodingProtobufJsonWriter.newBuilder().withTypeRegistry(TYPE_REGISTRY).build())
            .build();

    @Test
    public void ignoresDifferencesInEscaping() {
        TestAllTypesProto3 message = TestAllTypesProto3.newBuilder().setOptionalString("a=b<c>&'d\n").build();

        JsonFormatComparison.Result result = comparison.compare(message);

        // JsonFormat escapes some characters which do not need escaping
        assertThat(result.getExpected()).contains("\\u003d");
        assertThat(result.getActual()).contains("a=b");
        assertThat(result.isSame()).isTrue();
        assertThat(result.getDifferences()).isEmpty();

        comparison.verify(message);
    }

    @Test
    public void reportsWhenOnlyJsonFormatFails() {
        TestAllTypesProto3 message = TestAllTypesProto3.newBuilder()
                .setOptionalValue(Value.newBuilder().setNumberValue(Double.NaN))
                .build();

        JsonFormatComparison.Result result = comparison.compare(message);

        assertThat(result.isSame()).isFalse();
        assertThat(result.getExpectedFailure()).isNotNull();
        assertThat(result.getActual()).isEqualTo("{\"optionalValue\":\"NaN\"}");
        assertThat(result.getDifferences().get(0)).startsWith("JsonFormat failed");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> comparison.verify(message));
        assertThat(e).hasMessageThat().contains("JsonFormat failed");
    }

    @Test
    public void bothFailingIsTheSame() {
        TestAllTypesProto3 message = TestAllTypesProto3.newBuilder()
                .setOptionalAny(Any.pack(TestAllTypesProto2.getDefaultInstance()))
                .build();

        JsonFormatComparison.Result result = comparison.compare(message);

        assertThat(result.getExpectedFailure()).isNotNull();
        assertThat(result.getActualFailure()).isNotNull();
        assertThat(result.isSame()).isTrue();
    }

    @Test
    public void codedOutputStream() {
        JsonFormatComparison codedOutputStream = JsonFormatComparison.newBuilder()
                .withTypeRegistry(TYPE_REGISTRY)
                .withWriter(CodedOutputStreamProtobufJsonWriter.newBuilder().withTypeRegistry(TYPE_REGISTRY).build())
                .build();

        codedOutputStream.verify(new RandomMessages(1).fill(TestAllTypesProto3.newBuilder()));
    }
}
