package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpMessage;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

public abstract class AbstractSingleFieldAppendingMarker<T extends HttpMessage> extends SingleFieldAppendingMarker {

    protected final boolean validateJsonBody;

    protected final T message;
    public AbstractSingleFieldAppendingMarker(String markerName,  boolean validateJsonBody, T message) {
        super(markerName, "http");
        this.validateJsonBody = validateJsonBody;
        this.message = message;
    }

    protected void writeBody(JsonGenerator generator) {
        if(ContentType.isJsonMediaType(message.getContentType())) {
            // check that body is well-formed

            try {
                String bodyAsString = message.getBodyAsString();

                JsonFactory factory = generator.getCodec().getFactory();

                boolean escape = validateJsonBody && !isWellformedJson(bodyAsString, factory);

                if (escape) {
                    // escape body as string for debugging
                    generator.writeStringField("body", bodyAsString);
                } else {
                    generator.writeFieldName("body");
                    writeApprovedBody(generator, bodyAsString);
                }
            } catch(Exception e) {
                // should never happen, this is probably going to blow up somewhere else
            }
        }
        // omit writing body
    }

    protected void writeApprovedBody(JsonGenerator generator, String bodyAsString) throws IOException {
        generator.writeRawValue(bodyAsString);
    }

    protected boolean isWellformedJson(String s, JsonFactory jsonFactory) {
        try (JsonParser parser = jsonFactory.createParser(new StringReader(s))) {
            while(parser.nextToken() != null);
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    protected void writeHeaders(JsonGenerator generator) throws IOException {
        Map<String, List<String>> headers = message.getHeaders();
        if (!headers.isEmpty()) {
            // implementation note:
            // for some unclear reason, manually iterating over the headers
            // while writing performs worse than letting Jackson do the job.
            generator.writeObjectField("headers", headers);
        }
    }

    @Override
    protected T getFieldValue() {
        return message;
    }
}
