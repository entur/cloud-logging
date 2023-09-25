package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonGenerator;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;
import java.util.Locale;

public class ResponseSingleFieldAppendingMarker extends AbstractSingleFieldAppendingMarker<HttpResponse> {

    public static final String MARKER_NAME = ResponseSingleFieldAppendingMarker.MARKER_NAME_PREFIX + "RESPONSE";

    private final long duration;
    private String origin;
    private String protocol;
    private int status;

    public ResponseSingleFieldAppendingMarker(HttpResponse response, long duration, String body, boolean wellformed) {
        super(MARKER_NAME, response, body, wellformed);
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
