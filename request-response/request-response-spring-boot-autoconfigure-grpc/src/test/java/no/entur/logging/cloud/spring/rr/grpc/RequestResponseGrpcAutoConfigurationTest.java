package no.entur.logging.cloud.spring.rr.grpc;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.ErrorInfo;
import no.entur.logging.cloud.protobuf.json.ProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.codedoutputstream.CodedOutputStreamProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.jsonformat.JsonFormatProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter;
import no.entur.logging.cloud.rr.grpc.mapper.TypeRegistryFactory;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RequestResponseGrpcAutoConfigurationTest {

    private final JsonFormat.TypeRegistry typeRegistry = TypeRegistryFactory.createDefaultTypeRegistry();

    @Test
    public void createJsonWriters() throws Exception {
        assertThat(RequestResponseGrpcAutoConfiguration.createProtobufJsonWriter("json-format", typeRegistry)).isInstanceOf(JsonFormatProtobufJsonWriter.class);
        assertThat(RequestResponseGrpcAutoConfiguration.createProtobufJsonWriter("transcoding", typeRegistry)).isInstanceOf(TranscodingProtobufJsonWriter.class);
        assertThat(RequestResponseGrpcAutoConfiguration.createProtobufJsonWriter("coded-output-stream", typeRegistry)).isInstanceOf(CodedOutputStreamProtobufJsonWriter.class);
        assertThat(RequestResponseGrpcAutoConfiguration.createProtobufJsonWriter("CODED_OUTPUT_STREAM", typeRegistry)).isInstanceOf(CodedOutputStreamProtobufJsonWriter.class);

        ErrorInfo message = ErrorInfo.newBuilder().setReason("reason").putMetadata("key", "value").build();
        for (String name : new String[]{"json-format", "transcoding", "coded-output-stream"}) {
            ProtobufJsonWriter writer = RequestResponseGrpcAutoConfiguration.createProtobufJsonWriter(name, typeRegistry);
            assertThat(writer.writeAsString(message)).isEqualTo("{\"reason\":\"reason\",\"metadata\":{\"key\":\"value\"}}");
            assertThat(writer.writeAsString(Timestamp.newBuilder().setSeconds(1).build())).isEqualTo("\"1970-01-01T00:00:01Z\"");
        }
    }

    @Test
    public void failsForUnknownJsonWriter() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> RequestResponseGrpcAutoConfiguration.createProtobufJsonWriter("unknown", typeRegistry));
        assertThat(e).hasMessageThat().contains("unknown");
    }
}
