package no.entur.logging.cloud.logbook.logbook.test.ondemand;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.logbook.ondemand.HttpMessageBodyWriter;

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
        generator.writeFieldName("body");

        PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
        if (prettyPrinter == null) {
            generator.writeRawValue(new String(input, StandardCharsets.UTF_8));
        } else {
            final JsonFactory factory = generator.getCodec().getFactory();

            // append to existing tree event by event
            try (final JsonParser parser = factory.createParser(input)) {
                while (parser.nextToken() != null) {
                    generator.copyCurrentEvent(parser);
                }
            }
        }
    }
}
