package no.entur.logging.cloud.logbook.logbook.test;

import tools.jackson.core.JsonGenerator;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import org.zalando.logbook.HttpRequest;
import tools.jackson.core.JsonParser;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.TokenStreamFactory;

public class PrettyPrintingRequestSingleFieldAppendingMarker extends RequestSingleFieldAppendingMarker {

    public PrettyPrintingRequestSingleFieldAppendingMarker(HttpRequest request, String body, boolean wellformed) {
        super(request, body, wellformed);
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
