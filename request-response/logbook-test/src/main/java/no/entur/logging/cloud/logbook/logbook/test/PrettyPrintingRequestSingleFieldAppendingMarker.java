package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputMarker;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputType;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class PrettyPrintingRequestSingleFieldAppendingMarker extends ConsoleOutputTypeRequestMarker implements CompositeConsoleOutputMarker {

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
