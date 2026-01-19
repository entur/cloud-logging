package no.entur.logging.cloud.rr.grpc.test;

import tools.jackson.core.JsonGenerator;
import no.entur.logging.cloud.rr.grpc.marker.GrpcRequestMarker;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import tools.jackson.core.JsonParser;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;

import java.io.IOException;

public class PrettyPrintingRequestSingleFieldAppendingMarker extends GrpcRequestMarker {


    public PrettyPrintingRequestSingleFieldAppendingMarker(GrpcRequest message) {
        super(message);
    }

    @Override
    protected void writeBodyField(JsonGenerator generator, String body) {
        generator.writeName("body");

        final PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
        if (prettyPrinter == null || isRawStringValue(body)) {
            generator.writeRawValue(body);
        } else {
            final TokenStreamFactory factory = generator.objectWriteContext().tokenStreamFactory();

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
