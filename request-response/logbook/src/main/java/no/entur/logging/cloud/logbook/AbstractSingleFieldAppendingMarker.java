package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AbstractSingleFieldAppendingMarker<T extends HttpMessage> extends SingleFieldAppendingMarker {

    protected String contentType;
    protected Map<String, List<String>> headers;
    protected String body;
    protected boolean wellformed;

    public AbstractSingleFieldAppendingMarker(String markerName, T message, String body, boolean wellformed) {
        super(markerName, "http");
        this.body = body;
        this.wellformed = wellformed;

        prepareForDeferredProcessing(message);
    }

    protected void prepareForDeferredProcessing(T message) {
        // some value as read directly from underlying web object, which might be recycled before
        // this marker is written
        // capture all interesting fields

        contentType = message.getContentType();
        headers = message.getHeaders();
    }

    protected void writeBody(JsonGenerator generator) {
        if(body != null) {
            if (ContentType.isJsonMediaType(contentType)) {
                try {
                    if (wellformed) {
                        writeWellformedBody(generator);
                    } else {
                        generator.writeStringField("body", body);
                    }
                } catch (Exception e) {
                    // should never happen, this is probably going to blow up somewhere else
                }
            } else {
                try {
                    generator.writeStringField("body", body);
                } catch (Exception e) {
                    // should never happen, this is probably going to blow up somewhere else
                }
            }
        }
    }

    protected void writeWellformedBody(JsonGenerator generator) throws IOException {
        generator.writeFieldName("body");
        generator.writeRawValue(body);
    }

    protected void writeHeaders(JsonGenerator generator) throws IOException {
        generator.writeFieldName("headers");
        generator.writeStartObject();
        if(headers != null) {
            for (Map.Entry<String, List<String>> stringListEntry : headers.entrySet()) {

                String key = stringListEntry.getKey();
                if(key != null && !key.isEmpty()) {
                    generator.writeFieldName(key.toLowerCase());
                    generator.writeStartArray();

                    List<String> values = stringListEntry.getValue();
                    if(values != null) {
                        for (String value : values) {
                            generator.writeString(value);
                        }
                    }
                    generator.writeEndArray();
                }
            }
        }
        generator.writeEndObject();
    }

    @Override
    protected T getFieldValue() {
        return null;
    }

}
