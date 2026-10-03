package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Status;

/**
 * Map grpc status by using standard JsonPrinter with type registry supporting relevant Any types.
 */
public class JsonPrinterStatusMapper implements GrpcStatusMapper {
    private JsonFormat.Printer printer;

    public JsonPrinterStatusMapper(JsonFormat.Printer printer) {
        this.printer = printer;
    }

    /**
     * Map grpc status to JSON.
     *
     * @param status input status
     * @return the status as {@linkplain RawJson}, or an error message (String) if the status could not be printed.
     */

    @Override
    public Object map(Status status) {
        try {
            return new RawJson(printer.print(status));
        } catch (InvalidProtocolBufferException e) {
            return "[logging interceptor could not print status: " + e.getMessage() + "]";
        }
    }
}
