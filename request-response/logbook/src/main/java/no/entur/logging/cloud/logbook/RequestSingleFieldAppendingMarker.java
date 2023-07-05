package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpRequest;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RequestSingleFieldAppendingMarker extends AbstractSingleFieldAppendingMarker<HttpRequest> {

    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "REQUEST";

    public RequestSingleFieldAppendingMarker(HttpRequest request, boolean validateJsonBody) {
        super(MARKER_NAME, validateJsonBody, request);
    }

    @Override
    protected void writeFieldValue(JsonGenerator generator) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("origin", message.getOrigin().name().toLowerCase(Locale.ROOT));
        generator.writeStringField("type", "request");
        generator.writeStringField("protocol", message.getProtocolVersion());
        generator.writeStringField("remote", message.getRemote());
        generator.writeStringField("method", message.getMethod());
        generator.writeStringField("uri", message.getRequestUri());
        generator.writeStringField("host", message.getHost());
        generator.writeStringField("path", message.getPath());
        generator.writeStringField("scheme", message.getScheme());
        Optional<Integer> port = message.getPort();
        if(port.isEmpty()) {
            generator.writeNumberField("port", 80);
        } else {
            generator.writeNumberField("port", port.get());
        }

        writeHeaders(generator);
        writeBody(generator);

        generator.writeEndObject();
    }
}
