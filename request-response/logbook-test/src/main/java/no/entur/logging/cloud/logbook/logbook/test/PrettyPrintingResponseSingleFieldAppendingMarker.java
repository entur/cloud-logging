package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import no.entur.logging.cloud.logbook.ResponseSingleFieldAppendingMarker;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;

public class PrettyPrintingResponseSingleFieldAppendingMarker extends ResponseSingleFieldAppendingMarker {

public PrettyPrintingResponseSingleFieldAppendingMarker(HttpResponse response, long duration, boolean validateJsonBody) {
        super(response, duration, validateJsonBody);
    }

    @Override
    protected void writeApprovedBody(JsonGenerator generator, String bodyAsString) throws IOException {
        final PrettyPrinter prettyPrinter = generator.getPrettyPrinter();

        if (prettyPrinter == null) {
            super.writeFieldValue(generator);
        } else {
            final JsonFactory factory = generator.getCodec().getFactory();

            // append to existing tree event by event
            try (final JsonParser parser = factory.createParser(super.getFieldValue().toString())) {
                while (parser.nextToken() != null) {
                    generator.copyCurrentEvent(parser);
                }
            }
        }

    }
}
