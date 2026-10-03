package no.entur.logging.cloud.protobuf.json.jsonformat;

import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import no.entur.logging.cloud.protobuf.json.ProtobufJsonWriter;
import tools.jackson.core.JsonGenerator;

import java.io.IOException;

/**
 * {@linkplain ProtobufJsonWriter} using {@linkplain JsonFormat}. The default implementation.
 */

public class JsonFormatProtobufJsonWriter implements ProtobufJsonWriter {

    /**
     * @param typeRegistry types for resolving google.protobuf.Any
     * @return writer which omits insignificant whitespace
     */

    public static JsonFormatProtobufJsonWriter create(JsonFormat.TypeRegistry typeRegistry) {
        return new JsonFormatProtobufJsonWriter(JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace());
    }

    private final JsonFormat.Printer printer;

    public JsonFormatProtobufJsonWriter(JsonFormat.Printer printer) {
        this.printer = printer;
    }

    @Override
    public void write(MessageOrBuilder message, JsonGenerator generator) throws IOException {
        generator.writeRawValue(writeAsString(message));
    }

    @Override
    public String writeAsString(MessageOrBuilder message) throws IOException {
        int size = message instanceof MessageLite messageLite ? messageLite.getSerializedSize() * 4 : 256;
        StringBuilder builder = new StringBuilder(Math.max(size, 32));
        printer.appendTo(message, builder);
        return builder.toString();
    }

    public JsonFormat.Printer getPrinter() {
        return printer;
    }
}
