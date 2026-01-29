package no.entur.logging.cloud.logbook.util;

import com.fasterxml.jackson.core.*;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.IOException;
import java.util.function.LongSupplier;

/**
 * Thread-safe filter for JSON fields.
 * Please note that this filter will truncate and close the JSON after the given length is exceeded.
 * This may lead to confusion when looking at the logs if not aware.
 * Also note that the final JSON will not be exactly the maxBodySize specified - it depends on the JSON structure.
 */

public class MaxSizeJsonFilter {

    public static MaxSizeJsonFilter newInstance(int maxBodySize, JsonFactory jsonFactory) {
        return new MaxSizeJsonFilter(maxBodySize, jsonFactory);
    }

    private JsonFactory factory;
    private final int maxSize;

    public MaxSizeJsonFilter(int maxSize, JsonFactory jsonFactory) {
        this.maxSize = maxSize;
        this.factory = jsonFactory;
    }

    public String transform(String body) throws IOException {
        StringBuilder output = new StringBuilder(maxSize + 128);
        try (
                final JsonParser parser = factory.createParser(body);
                StringBuilderWriter writer = new StringBuilderWriter(output);
                JsonGenerator generator = factory.createGenerator(writer);
        ) {
            process(parser, generator, () -> generator.getOutputBuffered() + output.length());

            generator.close();
            return writer.toString();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String transform(byte[] body) throws IOException {
        StringBuilder output = new StringBuilder(maxSize + 128);
        try (
                final JsonParser parser = factory.createParser(body);
                StringBuilderWriter writer = new StringBuilderWriter(output);
                JsonGenerator generator = factory.createGenerator(writer);
        ) {
            process(parser, generator, () -> generator.getOutputBuffered() + output.length());

            generator.close();
            return writer.toString();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void process(final JsonParser parser, JsonGenerator generator, LongSupplier outputSizeSupplier) throws IOException {
        String message = "Max body size of " + maxSize + " reached, rest of the document has been filtered.";

        final long maxSize = this.maxSize - message.length() - 8;

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

            if(outputSize + size >= maxSize) {
                // write notification

                JsonStreamContext ctxt = generator.getOutputContext();
                if (ctxt.inArray()) {
                    generator.writeString("Logger: " + message);
                } else if (ctxt.inObject()) {
                    generator.writeStringField("Logger", message);
                }

                break;
            }

            if(fieldName != null) {
                generator.writeFieldName(fieldName);
                fieldName = null;
            }

            generator.copyCurrentEvent(parser);

            inputOffset = nextInputOffset;
        }
    }

    public int getMaxSize() {
        return maxSize;
    }
}
