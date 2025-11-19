package no.entur.logging.cloud.logbook.ondemand;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import no.entur.logging.cloud.appender.scope.LoggingScopePostProcessing;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AbstractOndemandSingleFieldAppendingMarker<T extends HttpMessage> extends SingleFieldAppendingMarker implements LoggingScopePostProcessing {

    protected String contentType;
    protected Map<String, List<String>> headers;

    // can be null
    protected HttpMessageBodyWriter httpMessageBodyWriter;

    public AbstractOndemandSingleFieldAppendingMarker(String markerName, T message, HttpMessageBodyWriter httpMessageBodyWriter) {
        super(markerName, "http");
        this.httpMessageBodyWriter = httpMessageBodyWriter;

        prepareForDeferredProcessing(message);
    }

    protected void prepareForDeferredProcessing(T message) {
        // some value as read directly from underlying web object, which might be recycled before
        // this marker is written
        // capture all interesting fields

        contentType = message.getContentType();
        headers = message.getHeaders();
    }

    public void performPostProcessing() {
        if(httpMessageBodyWriter != null) {
            httpMessageBodyWriter.prepareResult();
        }
    }

    protected void writeBody(JsonGenerator generator) {
        if(httpMessageBodyWriter != null) {
            // check that body is well-formed and within size

            // TODO get the generator.getOutputTarget() and calculate the size allocated to
            // fixed fields and headers
            // using generator.getOutputBuffered() + output.length()
            // while there technically can be multiple markers involved here,
            // this is not the case for these logbook-related statements

            try {
                httpMessageBodyWriter.writeBody(generator);
            } catch(Exception e) {
                // should never happen, this is probably going to blow up somewhere else
            }
        }
        // omit writing body
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
