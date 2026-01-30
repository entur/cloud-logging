package no.entur.logging.cloud.logbook.logbook.test;

import tools.jackson.core.JsonGenerator;
import no.entur.logging.cloud.logbook.ResponseSingleFieldAppendingMarker;
import org.zalando.logbook.HttpResponse;
import tools.jackson.core.JsonParser;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;

import java.io.IOException;
import java.time.Duration;

public class PrettyPrintingResponseSingleFieldAppendingMarker extends ResponseSingleFieldAppendingMarker {


    public PrettyPrintingResponseSingleFieldAppendingMarker(HttpResponse response, Duration duration, String body, boolean wellformed) {
        super(response, duration, body, wellformed);
    }

    @Override
    protected void writeWellformedBody(JsonGenerator generator) {
        final PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
        generator.writeName("body");
        if (prettyPrinter == null) {
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


}
