package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import org.zalando.logbook.HttpRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

public class PrettyPrintingRequestSingleFieldAppendingMarker extends RequestSingleFieldAppendingMarker {

    public PrettyPrintingRequestSingleFieldAppendingMarker(HttpRequest request, String body, boolean wellformed) {
        super(request, body, wellformed);
    }

    @Override
    protected void writeWellformedBody(JsonGenerator generator) throws IOException {
        final PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
        generator.writeFieldName("body");
        if (prettyPrinter == null) {
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

}
