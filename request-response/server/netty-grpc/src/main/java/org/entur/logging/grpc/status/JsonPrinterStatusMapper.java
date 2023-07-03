package org.entur.logging.grpc.status;

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

    @Override
    public String map(Status status) {
        try {
            return printer.print(status);
        } catch (InvalidProtocolBufferException e) {
            return "[logging interceptor could not print status: " + e.getMessage() + "]";
        }
    }
}
