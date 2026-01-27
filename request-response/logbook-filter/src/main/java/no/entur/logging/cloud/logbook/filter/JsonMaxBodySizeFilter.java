package no.entur.logging.cloud.logbook.filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import no.entur.logging.cloud.logbook.util.MaxSizeJsonFilter;
import org.apache.commons.io.output.StringBuilderWriter;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.ContentType;

import java.io.IOException;
import java.util.function.LongSupplier;

/**
 * Thread-safe filter for JSON fields.
 * Please note that this filter will truncate and close the JSON after the given length is exceeded.
 * This may lead to confusion when looking at the logs if not aware.
 * Also note that the final JSON will not be exactly the maxBodySize specified - it depends on the JSON structure.
 * <br>
 * Note: This filter assumes all local requests produce valid JSON if mime-type is JSON.
 */

public class JsonMaxBodySizeFilter implements BodyFilter {

    private final int maxBodySize;
    private final MaxSizeJsonFilter maxBodyFilter;

    public static JsonMaxBodySizeFilter newInstance(int maxBodySize) {
        return new JsonMaxBodySizeFilter(maxBodySize);
    }

    public JsonMaxBodySizeFilter(int maxBodySize) {
        this.maxBodySize = maxBodySize;
        this.maxBodyFilter = new MaxSizeJsonFilter(maxBodySize, new MappingJsonFactory());
    }

    @Override
    public String filter(String contentType, String body) {
        return ContentType.isJsonMediaType(contentType) ? filter(body) : body;
    }

    public String filter(final String body) {
        if(body.length() > maxBodySize) {
            try {
                return maxBodyFilter.transform(body);
            } catch (Exception e) {
                // NO-OP
                return body.substring(0, maxBodySize);
            }
        }
        return body;
    }

}
