package no.entur.logging.cloud.logbook.ondemand;

import tools.jackson.core.JsonGenerator;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

public class ResponseOndemandSingleFieldAppendingMarker extends AbstractOndemandSingleFieldAppendingMarker<HttpResponse> {

    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "RESPONSE";

    private final Duration duration;
    private String origin;
    private String protocol;
    private int status;

    public ResponseOndemandSingleFieldAppendingMarker(HttpResponse response, Duration duration, HttpMessageBodyWriter httpMessageBodyWriter) {
        super(MARKER_NAME, response, httpMessageBodyWriter);
        this.duration = duration;
    }

    @Override
    protected void prepareForDeferredProcessing(HttpResponse message) {
        super.prepareForDeferredProcessing(message);

        Origin origin = message.getOrigin();
        if(origin != null) {
            this.origin = origin.name().toLowerCase(Locale.ROOT);
        }
        protocol = message.getProtocolVersion();
        status = message.getStatus();
    }

    @Override
    protected void writeFieldValue(JsonGenerator generator) {
        generator.writeStartObject();

        if(origin != null) {
            generator.writeStringProperty("origin", origin);
        }
        generator.writeStringProperty("type", "response");
        if(duration != null) {
            generator.writeNumberProperty("duration", duration.toMillis());
        }
        if(protocol != null) {
            generator.writeStringProperty("protocol", protocol);
        }
        generator.writeNumberProperty("status", status);

        writeHeaders(generator);
        writeBody(generator);

        generator.writeEndObject();
    }

}
