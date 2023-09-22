package no.entur.logging.cloud.logbook.logbook.test.async;

import com.fasterxml.jackson.core.*;
import no.entur.logging.cloud.logbook.async.HttpMessageBodyWriter;
import no.entur.logging.cloud.logbook.async.state.HttpMessageStateOutput;
import no.entur.logging.cloud.logbook.util.MaxSizeJsonFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PrettyPrintingMaxSizeHttpMessageBodyWriter implements HttpMessageBodyWriter {

    protected final byte[] input;
    protected final int maxSize;
    protected final JsonFactory jsonFactory;

    protected HttpMessageStateOutput output;

    public PrettyPrintingMaxSizeHttpMessageBodyWriter(JsonFactory jsonFactory, byte[] input, int maxSize) {
        this.jsonFactory = jsonFactory;
        this.input = input;
        this.maxSize = maxSize;
    }

    public void prepareWriteBody() {
        output = createOutput();
    }

    protected HttpMessageStateOutput createOutput() {
        // do nothing
        // must parse body regardless, so do filtering and well-formed validation in a single operation
        String wellformed = filterMaxSize(input);

        if (wellformed != null) {
            return new HttpMessageStateOutput(true, wellformed);
        } else {
            String result = new String(input, 0, maxSize, StandardCharsets.UTF_8);
            return new HttpMessageStateOutput(false, result);
        }
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        if(output == null) {
            prepareWriteBody();
        }
        HttpMessageStateOutput output = this.output;

        if(output.isWellformed()) {
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
        } else {
            generator.writeStringField("body", output.getOutput());
        }
    }

    protected String filterMaxSize(byte[] body) {
        MaxSizeJsonFilter filter = new MaxSizeJsonFilter(maxSize, jsonFactory);

        try {
            return filter.transform(body);
        } catch (Exception e) {
            // NO-OP
            return null;
        }
    }

}
