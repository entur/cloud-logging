package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpRequest;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class PrettyPrintingRequestSingleFieldAppendingMarker extends RequestSingleFieldAppendingMarker {

    public PrettyPrintingRequestSingleFieldAppendingMarker(HttpRequest request, boolean validateJsonBody) {
        super(request, validateJsonBody);
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
