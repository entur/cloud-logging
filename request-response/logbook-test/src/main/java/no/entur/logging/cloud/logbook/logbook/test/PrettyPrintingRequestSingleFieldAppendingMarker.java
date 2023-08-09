package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import org.zalando.logbook.HttpRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PrettyPrintingRequestSingleFieldAppendingMarker extends RequestSingleFieldAppendingMarker {

    public PrettyPrintingRequestSingleFieldAppendingMarker(HttpRequest request, boolean validateJsonBody, int maxBodySize, int maxSize) {
        super(request, validateJsonBody, maxBodySize, maxSize);
    }

    @Override
    protected void writeApprovedBody(JsonGenerator generator, byte[] body) throws IOException {
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
