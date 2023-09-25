package no.entur.logging.cloud.logbook.ondemand;

import com.fasterxml.jackson.core.*;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateResult;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.LongSupplier;

public class MaxSizeLocalHttpMessageBodyWriter implements HttpMessageBodyWriter {

    protected final byte[] input;
    protected final int maxSize;
    protected final JsonFactory jsonFactory;

    protected HttpMessageStateResult output;

    public MaxSizeLocalHttpMessageBodyWriter(JsonFactory jsonFactory, byte[] input, int maxSize) {
        this.jsonFactory = jsonFactory;
        this.input = input;
        this.maxSize = maxSize;
    }

    public void prepareResult() {
        output = createOutput();
    }

    protected HttpMessageStateResult createOutput() {
        // do nothing
        // must parse body regardless, so do filtering and well-formed validation in a single operation
        String wellformed = filterMaxSize(input, maxSize);

        if (wellformed != null) {
            return new HttpMessageStateResult(true, wellformed);
        } else {
            String result = new String(input, 0, Math.min(maxSize, input.length), StandardCharsets.UTF_8);
            return new HttpMessageStateResult(false, result);
        }
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        if(output == null) {
            prepareResult();
        }
        HttpMessageStateResult output = this.output;

        if(output.isWellformed()) {
            generator.writeFieldName("body");
            generator.writeRawValue(output.getBody());
        } else {
            generator.writeStringField("body", output.getBody());
        }
    }

    protected String filterMaxSize(byte[] body, int max) {
        try (
                JsonParser parser = jsonFactory.createParser(body);
                CharArrayWriter writer = new CharArrayWriter(max);
                JsonGenerator generator = jsonFactory.createGenerator(writer);
        ) {
            process(parser, generator, () -> generator.getOutputBuffered() + writer.size(), max);

            generator.flush();
            return writer.toString();
        } catch (Exception e) {
            // NO-OP
            return null;
        }
    }

    public void process(final JsonParser parser, JsonGenerator generator, LongSupplier outputSizeSupplier, int max) throws IOException {
        String message = "Max body size of " + max + " reached, rest of the document has been filtered.";

        final long limit = max - message.length() - 8 - 16; // filtering is not completely accurate

        // do not write field name if field value is too long
        String fieldName = null;

        long inputOffset = 0;

        while(true) {
            JsonToken nextToken = parser.nextToken();
            if(nextToken == null) {
                break;
            }
            if(nextToken == JsonToken.FIELD_NAME) {
                fieldName = parser.currentName();

                continue;
            }

            if(nextToken == JsonToken.VALUE_STRING) {
                parser.getTextLength();
            }

            long nextInputOffset = parser.currentLocation().getCharOffset();

            long size;
            if(nextToken == JsonToken.VALUE_STRING) {
                size = parser.getTextLength() + 2;
                if(fieldName != null) {
                    size += fieldName.length() + 2;
                }
            } else {
                size = nextInputOffset - inputOffset; // i.e. this includes whitespace
            }
            long outputSize = outputSizeSupplier.getAsLong();

            if(outputSize + size >= limit) {
                // write notification

                JsonStreamContext ctxt = generator.getOutputContext();
                if (ctxt.inArray()) {
                    generator.writeString("Logger: " + message);
                } else if (ctxt.inObject()) {
                    generator.writeStringField("Logger", message);
                }

                generator.close();

                break;
            }

            if(fieldName != null) {
                generator.writeFieldName(fieldName);
                fieldName = null;
            }

            generator.copyCurrentEvent(parser);

            inputOffset = nextInputOffset;
        }
        generator.flush();
    }

}
