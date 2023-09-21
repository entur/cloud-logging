package no.entur.logging.cloud.rr.grpc.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.rr.grpc.marker.GrpcResponseMarker;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import java.io.IOException;

public class PrettyPrintingResponseSingleFieldAppendingMarker extends GrpcResponseMarker {

    public PrettyPrintingResponseSingleFieldAppendingMarker(GrpcResponse message) {
        super(message);
    }

    @Override
    protected void writeBodyField(JsonGenerator generator, String body) throws IOException {
        generator.writeFieldName("body");

        final PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
        if (prettyPrinter == null || isRawStringValue(body)) {
            generator.writeRawValue(body);
        } else {
            final JsonFactory factory = generator.getCodec().getFactory();

            // append to existing tree event by event
            try (final JsonParser parser = factory.createParser(body)) {
                while (parser.nextToken() != null) {
                    generator.copyCurrentEvent(parser);
                }
            }
        }
    }


    private static boolean isRawStringValue(String body) {
        return body.charAt(0) == '"' && body.charAt(body.length() - 1) == '"';
    }

}
