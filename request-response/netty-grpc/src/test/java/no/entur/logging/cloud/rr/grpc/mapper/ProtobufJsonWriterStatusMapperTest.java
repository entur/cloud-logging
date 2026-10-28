package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.Any;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import no.entur.logging.cloud.rr.grpc.filter.GrpcPayloadFilter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.util.RawValue;

import static com.google.common.truth.Truth.assertThat;

public class ProtobufJsonWriterStatusMapperTest {

    private final ErrorInfo errorInfoDetail = ErrorInfo.newBuilder().setDomain("ErrorDomain").setReason("errorReason").build();
    private final Status statusWithErrorInfo = Status.newBuilder().addDetails(Any.pack(errorInfoDetail)).build();

    @Test
    public void knownAnyTypeCanBePrinted() {
        ProtobufJsonWriterStatusMapper statusMapper = new ProtobufJsonWriterStatusMapper(new TranscodingProtobufJsonWriterFactory(TypeRegistryFactory.createDefaultTypeRegistry(), -1).create(GrpcPayloadFilter.NONE));

        Object result = statusMapper.map(statusWithErrorInfo);
        assertThat(result).isInstanceOf(RawValue.class);

        String statusJson = String.valueOf(((RawValue) result).rawValue());
        assertThat(statusJson).isEqualTo("{\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.ErrorInfo\",\"reason\":\"errorReason\",\"domain\":\"ErrorDomain\"}]}");
    }

    @Test
    public void unknownAnyTypeGivesErrorMessage() {
        ProtobufJsonWriterStatusMapper statusMapper = new ProtobufJsonWriterStatusMapper(new TranscodingProtobufJsonWriterFactory(JsonFormat.TypeRegistry.getEmptyTypeRegistry(), -1).create(GrpcPayloadFilter.NONE));

        Object statusJson = statusMapper.map(statusWithErrorInfo);
        assertThat(statusJson).isEqualTo("[logging interceptor could not print status: Cannot find type for url: type.googleapis.com/google.rpc.ErrorInfo]");
    }
}
