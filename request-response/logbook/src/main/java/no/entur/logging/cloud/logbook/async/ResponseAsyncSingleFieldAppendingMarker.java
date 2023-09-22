package no.entur.logging.cloud.logbook.async;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;
import java.util.Locale;

public class ResponseAsyncSingleFieldAppendingMarker extends AbstractAsyncSingleFieldAppendingMarker<HttpResponse> {

    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "RESPONSE";

    private final long duration;
    private String origin;
    private String protocol;
    private int status;

    public ResponseAsyncSingleFieldAppendingMarker(HttpResponse response, long duration, HttpMessageBodyWriter httpMessageBodyWriter) {
        super(MARKER_NAME, response, httpMessageBodyWriter);
        this.duration = duration;
    }

    @Override
    protected void prepareForDeferredProcessing(HttpResponse message) {
        super.prepareForDeferredProcessing(message);

        origin = message.getOrigin().name().toLowerCase(Locale.ROOT);
        protocol = message.getProtocolVersion();
        status = message.getStatus();
    }

    @Override
    protected void writeFieldValue(JsonGenerator generator) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("origin", origin);
        generator.writeStringField("type", "response");
        generator.writeNumberField("duration", duration);
        generator.writeStringField("protocol", protocol);
        generator.writeNumberField("status", status);

        writeHeaders(generator);
        writeBody(generator);

        generator.writeEndObject();
    }

}
