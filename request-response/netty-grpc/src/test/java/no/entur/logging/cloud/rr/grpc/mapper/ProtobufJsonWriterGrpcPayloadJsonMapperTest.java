package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.util.JsonFormat;
import no.entur.logging.cloud.rr.grpc.filter.GrpcPayloadFilter;
import no.entur.logging.cloud.rr.grpc.filter.NoneGrpcBodyFilter;
import org.entur.oidc.grpc.test.GreetingRequest;
import org.entur.oidc.grpc.test.GreetingResponse;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProtobufJsonWriterGrpcPayloadJsonMapperTest {

    private final ProtobufJsonWriterGrpcPayloadJsonMapper mapper = new ProtobufJsonWriterGrpcPayloadJsonMapper(new TranscodingProtobufJsonWriterFactory(TypeRegistryFactory.createDefaultTypeRegistry(), 1024));

    private final GreetingRequest request = GreetingRequest.newBuilder().setMessage("secret").setReturnMessageSize(5).build();

    @Test
    public void testSameAsJsonFormat() throws Exception {
        String expected = JsonFormat.printer().omittingInsignificantWhitespace().print(request);

        assertThat(mapper.map(request, NoneGrpcBodyFilter.getInstance())).isEqualTo(expected);
        assertThat(mapper.map(request, GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).getJson()).isEqualTo(expected);
        assertThat(mapper.map(request.toBuilder(), GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).getJson()).isEqualTo(expected);
    }

    @Test
    public void testAnonymize() throws Exception {
        GrpcPayloadFilter filter = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("message").build();

        assertThat(mapper.map(request, filter, NoneGrpcBodyFilter.getInstance()).getJson()).isEqualTo("{\"message\":\"*\",\"returnMessageSize\":\"5\"}");
    }

    @Test
    public void testPrune() throws Exception {
        GrpcPayloadFilter filter = GrpcPayloadFilter.newBuilder().withPrunePaths("$.message").build();

        assertThat(mapper.map(request, filter, NoneGrpcBodyFilter.getInstance()).getJson()).isEqualTo("{\"message\":\"[removed]\",\"returnMessageSize\":\"5\"}");
    }

    @Test
    public void testMaxStringLength() throws Exception {
        GrpcPayloadFilter filter = GrpcPayloadFilter.newBuilder().withMaxStringLength(3).build();

        GreetingRequest request = GreetingRequest.newBuilder().setMessage("abcdefghijklmnopqrstuvwxyz").build();

        assertThat(mapper.map(request, filter, NoneGrpcBodyFilter.getInstance()).getJson()).isEqualTo("{\"message\":\"abc... + 23\"}");
    }

    @Test
    public void testMaxSizeTruncatesToValidJson() throws Exception {
        GreetingResponse response = GreetingResponse.newBuilder().setStatus(5).setMessage("a".repeat(2048)).build();

        String json = mapper.map(response, NoneGrpcBodyFilter.getInstance());

        assertThat(json).isEqualTo("{\"status\":\"5\"}");
    }

    @Test
    public void testMaxSizeOmitsMessageIfFirstFieldDoesNotFit() throws Exception {
        GreetingRequest request = GreetingRequest.newBuilder().setMessage("a".repeat(2048)).setReturnMessageSize(5).build();

        assertThat(mapper.map(request, NoneGrpcBodyFilter.getInstance())).isEqualTo("\"Omitted binary message size " + request.getSerializedSize() + "\"");
        assertThat(mapper.map(request.toBuilder(), GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).getJson()).isEqualTo("\"Omitted message\"");
    }

    @Test
    public void testTruncatedIsReported() throws Exception {
        GreetingResponse large = GreetingResponse.newBuilder().setStatus(5).setMessage("a".repeat(2048)).build();
        GreetingResponse small = GreetingResponse.newBuilder().setStatus(5).setMessage("a".repeat(16)).build();
        GreetingRequest omitted = GreetingRequest.newBuilder().setMessage("a".repeat(2048)).build();
        GrpcPayloadFilter filter = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("other").build();

        assertThat(mapper.map(large, GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).isTruncated()).isTrue();
        assertThat(mapper.map(large, filter, NoneGrpcBodyFilter.getInstance()).isTruncated()).isTrue();
        assertThat(mapper.map(omitted, GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).isTruncated()).isTrue();

        assertThat(mapper.map(small, GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).isTruncated()).isFalse();
        assertThat(mapper.map(small, filter, NoneGrpcBodyFilter.getInstance()).isTruncated()).isFalse();
    }

    @Test
    public void testTruncatedIsNotReportedWithoutMaxSize() throws Exception {
        ProtobufJsonWriterGrpcPayloadJsonMapper mapper = new ProtobufJsonWriterGrpcPayloadJsonMapper(new TranscodingProtobufJsonWriterFactory(TypeRegistryFactory.createDefaultTypeRegistry(), -1));

        GreetingResponse large = GreetingResponse.newBuilder().setStatus(5).setMessage("a".repeat(2048)).build();
        GrpcPayloadFilter filter = GrpcPayloadFilter.newBuilder().withMaxStringLength(16).build();

        assertThat(mapper.map(large, GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).isTruncated()).isFalse();
        // max string length truncation is visible in the values themselves
        assertThat(mapper.map(large, filter, NoneGrpcBodyFilter.getInstance()).isTruncated()).isFalse();
    }

    @Test
    public void testEmpty() throws Exception {
        assertThat(mapper.map(GreetingRequest.getDefaultInstance(), NoneGrpcBodyFilter.getInstance())).isNull();
        assertThat(mapper.map(null, GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).getJson()).isNull();
    }

    @Test
    public void testWriterPerPayloadFilter() {
        GrpcPayloadFilter a = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("message").build();
        GrpcPayloadFilter b = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("message").build();
        GrpcPayloadFilter c = GrpcPayloadFilter.newBuilder().withPruneKeys("message").build();

        assertThat(mapper.getWriter(a)).isSameInstanceAs(mapper.getWriter(b));
        assertThat(mapper.getWriter(a)).isNotSameInstanceAs(mapper.getWriter(c));
        assertThat(mapper.getWriter(GrpcPayloadFilter.newBuilder().build())).isSameInstanceAs(mapper.getWriter(GrpcPayloadFilter.NONE));
    }

    @Test
    public void testMapperWithoutPayloadFilterSupportFailsForPayloadFilters() throws Exception {
        DefaultGrpcPayloadJsonMapper defaultMapper = new DefaultGrpcPayloadJsonMapper(JsonPrinterFactory.createPrinter(false, TypeRegistryFactory.createDefaultTypeRegistry()), 1024, 1024);

        GrpcPayloadFilter filter = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("message").build();

        assertThrows(IllegalStateException.class, () -> defaultMapper.map(request, filter, NoneGrpcBodyFilter.getInstance()));
        assertThat(defaultMapper.map(request, GrpcPayloadFilter.NONE, NoneGrpcBodyFilter.getInstance()).getJson()).isEqualTo(mapper.map(request, NoneGrpcBodyFilter.getInstance()));
    }
}
