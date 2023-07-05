package no.entur.logging.cloud.logbook.filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.common.MediaTypeQuery;

import java.io.StringWriter;
import java.util.function.Predicate;

/**
 * Thread-safe filter for JSON fields.
 * Please note that this filter will truncate and close the JSON after the given length is exceeded.
 * This may lead to confusion when looking at the logs if not aware.
 * Also note that the final JSON will not be exactly the maxBodySize specified - it depends on the JSON structure.
 */

public class JsonMaxBodySizeFilter implements BodyFilter {

    private static final Predicate<String> json = MediaTypeQuery.compile("application/json", "application/*+json");
    private final long maxBodySize;

    public static JsonMaxBodySizeFilter newInstance(int maxBodySize) {
        return new JsonMaxBodySizeFilter(maxBodySize);
    }

    private JsonFactory factory;

    public JsonMaxBodySizeFilter(long maxBodySize) {
        this.maxBodySize = maxBodySize;
        this.factory = new ObjectMapper().getFactory();
    }

    @Override
    public String filter(String contentType, String body) {
        return json.test(contentType) ? filter(body) : body;
    }

    public String filter(final String body) {
        try (
                final JsonParser parser = factory.createParser(body);
                StringWriter writer = new StringWriter(body.length());
                JsonGenerator generator = factory.createGenerator(writer);
        ) {
            JsonToken nextToken;
            long totalSize = 0;

            while ((nextToken = parser.nextToken()) != null) {
                if (nextToken.isStructStart()) {

                    if (totalSize > maxBodySize) {
                        generator.copyCurrentEvent(parser);
                        generator.writeStartObject();
                        generator.writeStringField("JsonMaxBodySizeFilterMessage", "This struct and the rest of the object has been filtered by logger.");
                        generator.close();
                        break;
                    }
                }

                generator.copyCurrentEvent(parser);
                totalSize = parser.currentLocation().getCharOffset();
            }
            generator.flush();
            return writer.toString();
        } catch (Exception e) {
            // NO-OP
        }
        return body;
    }
}
