package no.entur.logging.cloud.rr.grpc.marker;

import io.grpc.Status;
import no.entur.logging.cloud.rr.grpc.message.GrpcPayload;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.RawValue;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class GrpcConnectionMarkerTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    public void writesRawValueHeaderValueAsStructuredJson() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("grpc-status-details", new RawValue("{\"code\":3,\"message\":\"My error message\"}"));
        headers.put("grpc-message", "My error message");

        GrpcResponse response = new GrpcResponse(headers, null, "/my.Service/method", (GrpcPayload) null, "local", 1, Status.Code.INVALID_ARGUMENT, 10);

        JsonNode headersNode = write(new GrpcResponseMarker(response)).get("http").get("headers");

        JsonNode statusDetails = headersNode.get("grpc-status-details").get(0);
        assertThat(statusDetails.isObject()).isTrue();
        assertThat(statusDetails.get("code").asInt()).isEqualTo(3);
        assertThat(statusDetails.get("message").asString()).isEqualTo("My error message");

        assertThat(headersNode.get("grpc-message").get(0).asString()).isEqualTo("My error message");
    }

    private JsonNode write(GrpcConnectionMarker<?> marker) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = mapper.createGenerator(writer)) {
            generator.writeStartObject();
            marker.writeTo(generator);
            generator.writeEndObject();
        }
        return mapper.readTree(writer.toString());
    }
}
