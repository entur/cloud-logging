package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.util.JsonFormat;

public class JsonPrinterFactory {

    public static JsonFormat.Printer createPrinter(boolean prettyPrint, JsonFormat.TypeRegistry typeRegistry) {
        if (prettyPrint) {
            return JsonFormat.printer().usingTypeRegistry(typeRegistry);
        }
        return JsonFormat.printer().omittingInsignificantWhitespace().usingTypeRegistry(typeRegistry);
    }
}
