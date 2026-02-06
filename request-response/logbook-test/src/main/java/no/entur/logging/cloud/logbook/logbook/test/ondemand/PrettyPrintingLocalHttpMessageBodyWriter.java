package no.entur.logging.cloud.logbook.logbook.test.ondemand;

import no.entur.logging.cloud.logbook.ondemand.HttpMessageBodyWriter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PrettyPrintingLocalHttpMessageBodyWriter implements HttpMessageBodyWriter {

    protected final byte[] input;

    public PrettyPrintingLocalHttpMessageBodyWriter(byte[] input) {
        this.input = input;
    }

    @Override
    public void prepareResult() {
        // do nothing
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        generator.writeName("body");

        PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
        if (prettyPrinter == null) {
            generator.writeRawValue(new String(input, StandardCharsets.UTF_8));
        } else {
            final TokenStreamFactory factory = generator.objectWriteContext().tokenStreamFactory();

            // append to existing tree event by event
            try (final JsonParser parser = factory.createParser(input)) {
                while (parser.nextToken() != null) {
                    generator.copyCurrentEvent(parser);
                }
            }
        }
    }
}
