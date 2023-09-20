package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.logbook.ResponseSingleFieldAppendingMarker;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

public class PrettyPrintingResponseSingleFieldAppendingMarker extends ResponseSingleFieldAppendingMarker {

    public PrettyPrintingResponseSingleFieldAppendingMarker(HttpResponse response, long duration, BooleanSupplier validateJsonBody, int maxBodySize, int maxSize) {
        super(response, duration, validateJsonBody, maxBodySize, maxSize);
    }

    @Override
    protected void writeApprovedBody(JsonGenerator generator, byte[] bodyAsString) throws IOException {
        final PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
        if (prettyPrinter == null) {
            generator.writeRawValue(new String(body, StandardCharsets.UTF_8));
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
