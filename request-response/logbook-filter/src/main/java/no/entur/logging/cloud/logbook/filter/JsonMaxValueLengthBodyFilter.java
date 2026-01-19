package no.entur.logging.cloud.logbook.filter;

import tools.jackson.core.JsonGenerator;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.ContentType;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;
import java.util.function.Predicate;

/**
 * Thread-safe filter for JSON fields.
 * Please note that this filter will truncate long JSON values after the given length and replace the excess characters
 * with the string [filtered by logger] - which may lead to confusion when looking at the logs if not aware.
 */

public class JsonMaxValueLengthBodyFilter implements BodyFilter {

    private final int maxFieldLength;

    public static JsonMaxValueLengthBodyFilter newInstance(int maxFieldLength) {
        return new JsonMaxValueLengthBodyFilter(maxFieldLength);
    }

    private JsonMapper mapper;

    public JsonMaxValueLengthBodyFilter(int maxFieldLength) {
        this.maxFieldLength = maxFieldLength;
        this.mapper = JsonMapper.builder().build();
    }

    @Override
    public String filter(String contentType, String body) {
        return ContentType.isJsonMediaType(contentType) ? filter(body) : body;
    }

    public String filter(final String body) {
        try (
                final JsonParser parser = mapper.createParser(body);
                StringWriter writer = new StringWriter(body.length());
                JsonGenerator generator = mapper.createGenerator(writer);
        ) {
            JsonToken nextToken;

            while ((nextToken = parser.nextToken()) != null) {
                if (nextToken == JsonToken.VALUE_STRING) {
                    String valueAsString = parser.getValueAsString();
                    if(valueAsString.length() > maxFieldLength) {
                        generator.writePOJO(valueAsString.substring(0, maxFieldLength)+"[filtered by logger]");
                    } else {
                        generator.writePOJO(valueAsString);
                    }
                }
                else {
                    generator.copyCurrentEvent(parser);
                }
            }
            generator.flush();
            return writer.toString();
        } catch (Exception e) {
            // NO-OP
        }
        return body;
    }


}
