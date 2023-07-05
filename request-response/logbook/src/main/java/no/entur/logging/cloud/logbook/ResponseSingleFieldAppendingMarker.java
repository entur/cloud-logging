package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResponseSingleFieldAppendingMarker extends AbstractSingleFieldAppendingMarker<HttpResponse> {

    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "RESPONSE";

    private final long duration;

    public ResponseSingleFieldAppendingMarker(HttpResponse response, long duration, boolean validateJsonBody) {
        super(MARKER_NAME, validateJsonBody, response);
        this.duration = duration;
    }

    @Override
    protected void writeFieldValue(JsonGenerator generator) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("origin", message.getOrigin().name().toLowerCase(Locale.ROOT));
        generator.writeStringField("type", "request");
        generator.writeNumberField ("duration", duration);
        generator.writeStringField("protocol", message.getProtocolVersion());
        generator.writeNumberField("status", message.getStatus());

        writeHeaders(generator);
        writeBody(generator);

        generator.writeEndObject();
    }

}
