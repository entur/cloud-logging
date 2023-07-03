package org.entur.auth.grpc.status;

import com.google.protobuf.Any;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import org.entur.logging.grpc.JsonPrinterFactory;
import org.entur.logging.grpc.TypeRegistryFactory;
import org.entur.logging.grpc.status.JsonPrinterStatusMapper;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonPrinterStatusMapperTest {


    @Test
    public void knownAnyTypeCanBePrinted() {
        JsonPrinterStatusMapper statusMapper = new JsonPrinterStatusMapper(JsonPrinterFactory.createPrinter(true, TypeRegistryFactory.createDefaultTypeRegistry()));

        ErrorInfo errorInfoDetail = ErrorInfo.newBuilder().setDomain("ErrorDomain").setReason("errorReason").build();
        Status statusWithErrorInfo = Status.newBuilder().addDetails(Any.pack(errorInfoDetail)).build();


        String statusJson = statusMapper.map(statusWithErrorInfo);
        assertTrue(statusJson.contains(errorInfoDetail.getDomain()));
        assertTrue(statusJson.contains(errorInfoDetail.getReason()));
    }

    @Test
    public void unknownAnyTypeGivesErrorMessage() {
        JsonPrinterStatusMapper statusMapper = new JsonPrinterStatusMapper(JsonPrinterFactory.createPrinter(true, JsonFormat.TypeRegistry.getEmptyTypeRegistry()));

        ErrorInfo errorInfoDetail = ErrorInfo.newBuilder().setDomain("ErrorDomain").setReason("errorReason").build();
        Status statusWithErrorInfo = Status.newBuilder().addDetails(Any.pack(errorInfoDetail)).build();

        String statusJson = statusMapper.map(statusWithErrorInfo);
        assertEquals("[logging interceptor could not print status: Cannot find type for url: type.googleapis.com/google.rpc.ErrorInfo]", statusJson);
    }
}
