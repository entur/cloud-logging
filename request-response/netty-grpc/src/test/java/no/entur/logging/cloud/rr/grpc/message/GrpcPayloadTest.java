package no.entur.logging.cloud.rr.grpc.message;

import com.google.protobuf.InvalidProtocolBufferException;
import no.entur.logging.cloud.rr.grpc.filter.NoneGrpcBodyFilter;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import org.entur.oidc.grpc.test.GreetingRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GrpcPayloadTest {

    private final GreetingRequest request = GreetingRequest.newBuilder().setMessage("Hello").build();

    @Test
    public void mapsMessageOnlyOnce() {
        AtomicInteger count = new AtomicInteger();
        GrpcPayloadJsonMapper mapper = (m, filter) -> {
            count.incrementAndGet();
            return "{\"message\":\"Hello\"}";
        };

        GrpcPayload payload = new GrpcPayload(request, NoneGrpcBodyFilter.getInstance(), mapper);
        assertThat(payload.isMapped()).isFalse();
        assertThat(count.get()).isEqualTo(0);

        assertThat(payload.getBody()).isEqualTo("{\"message\":\"Hello\"}");
        assertThat(payload.getBody()).isEqualTo("{\"message\":\"Hello\"}");

        assertThat(payload.isMapped()).isTrue();
        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    public void mapRethrowsMappingFailure() {
        GrpcPayloadJsonMapper mapper = (m, filter) -> {
            throw new InvalidProtocolBufferException("Cannot find type");
        };

        GrpcPayload payload = new GrpcPayload(request, NoneGrpcBodyFilter.getInstance(), mapper);

        assertThrows(InvalidProtocolBufferException.class, payload::map);
        assertThat(payload.isMapped()).isTrue();
        assertThat(payload.getBody()).isNull();
    }

    @Test
    public void bodyDescribesMappingFailure() {
        GrpcPayloadJsonMapper mapper = (m, filter) -> {
            throw new InvalidProtocolBufferException("Cannot find type \"x\"");
        };

        GrpcPayload payload = new GrpcPayload(request, NoneGrpcBodyFilter.getInstance(), mapper);

        String body = payload.getBody();

        // body is a well-formed JSON string
        String value = JsonMapper.builder().build().readTree(body).asString();
        assertThat(value).isEqualTo("Unable to format message: Cannot find type \"x\"");
    }

    @Test
    public void createFromMappedBody() {
        GrpcPayload payload = GrpcPayload.of("{\"message\":\"Hello\"}");

        assertThat(payload.isMapped()).isTrue();
        assertThat(payload.getMessage()).isNull();
        assertThat(payload.getBody()).isEqualTo("{\"message\":\"Hello\"}");
    }

    @Test
    public void releasesMessageAfterMapping() {
        GrpcPayload payload = new GrpcPayload(request, NoneGrpcBodyFilter.getInstance(), (m, filter) -> "{}");
        assertThat(payload.getMessage()).isSameInstanceAs(request);

        payload.getBody();

        assertThat(payload.getMessage()).isNull();
    }

    @Test
    public void releasesMessageAfterMappingFailure() {
        GrpcPayload payload = new GrpcPayload(request, NoneGrpcBodyFilter.getInstance(), (m, filter) -> {
            throw new InvalidProtocolBufferException("fail");
        });

        assertThrows(InvalidProtocolBufferException.class, payload::map);

        assertThat(payload.getMessage()).isNull();
    }

    @Test
    public void snapshotsBuilder() {
        GreetingRequest.Builder builder = GreetingRequest.newBuilder().setMessage("Hello");
        // map the message which was passed to the mapper
        GrpcPayload payload = new GrpcPayload(builder, NoneGrpcBodyFilter.getInstance(), (m, filter) -> ((GreetingRequest) m).getMessage());

        builder.setMessage("Changed");

        assertThat(payload.getBody()).isEqualTo("Hello");
    }
}
