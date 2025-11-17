package no.entur.logging.cloud.logbook.ondemand;

import com.fasterxml.jackson.core.JsonGenerator;
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
    protected void writeFieldValue(JsonGenerator generator) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("origin", origin);
        generator.writeStringField("type", "response");
        if(duration != null) {
            generator.writeNumberField("duration", duration.toMillis());
        }
        generator.writeStringField("protocol", protocol);
        generator.writeNumberField("status", status);

        writeHeaders(generator);
        writeBody(generator);

        generator.writeEndObject();
    }

}
