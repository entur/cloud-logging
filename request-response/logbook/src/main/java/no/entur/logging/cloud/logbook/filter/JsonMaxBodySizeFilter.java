package no.entur.logging.cloud.logbook.filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.output.StringBuilderWriter;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.ContentType;

import java.io.IOException;
import java.io.StringWriter;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/**
 * Thread-safe filter for JSON fields.
 * Please note that this filter will truncate and close the JSON after the given length is exceeded.
 * This may lead to confusion when looking at the logs if not aware.
 * Also note that the final JSON will not be exactly the maxBodySize specified - it depends on the JSON structure.
 */

public class JsonMaxBodySizeFilter implements BodyFilter {

    private final int maxBodySize;

    public static JsonMaxBodySizeFilter newInstance(int maxBodySize) {
        return new JsonMaxBodySizeFilter(maxBodySize);
    }

    private JsonFactory factory;

    public JsonMaxBodySizeFilter(int maxBodySize) {
        this.maxBodySize = maxBodySize;
        this.factory = new ObjectMapper().getFactory();
    }

    @Override
    public String filter(String contentType, String body) {
        return ContentType.isJsonMediaType(contentType) ? filter(body) : body;
    }

    public String filter(final String body) {
        if(body.length() > maxBodySize) {
            StringBuilder output = new StringBuilder(maxBodySize);
            try (
                    final JsonParser parser = factory.createParser(body);
                    StringBuilderWriter writer = new StringBuilderWriter(maxBodySize + 128);
                    JsonGenerator generator = factory.createGenerator(writer);
            ) {
                process(parser, generator, () -> generator.getOutputBuffered() + output.length());

                generator.flush();
                return writer.toString();
            } catch (Exception e) {
                // NO-OP
                return body.substring(0, maxBodySize);
            }
        }
        return body;
    }

    public void process(final JsonParser parser, JsonGenerator generator, LongSupplier outputSizeSupplier) throws IOException {
        String message = "Max body size of " + maxBodySize + " reached, rest of the document has been filtered.";

        final long maxSize = this.maxBodySize - message.length() - 8;

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
