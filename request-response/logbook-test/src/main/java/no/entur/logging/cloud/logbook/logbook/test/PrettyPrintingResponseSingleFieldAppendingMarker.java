package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.logbook.ResponseSingleFieldAppendingMarker;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;

public class PrettyPrintingResponseSingleFieldAppendingMarker extends ResponseSingleFieldAppendingMarker {


    public PrettyPrintingResponseSingleFieldAppendingMarker(HttpResponse response, long duration, String body, boolean wellformed) {
        super(response, duration, body, wellformed);
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
