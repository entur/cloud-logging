package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.HttpRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class RequestSingleFieldAppendingMarker extends AbstractSingleFieldAppendingMarker<HttpRequest> {

    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "REQUEST";

    protected String origin;
    protected String protocol;
    protected String remote;
    protected String method;
    protected String uri;
    protected String host;
    protected String path;
    protected String scheme;
    protected Optional<Integer> port;

    public RequestSingleFieldAppendingMarker(HttpRequest request, BooleanSupplier wellformed, int maxBodySize, int maxSize) {
        super(MARKER_NAME, wellformed, request, maxBodySize, maxSize);
    }

    @Override
    protected void prepareForDeferredProcessing(HttpRequest message) {
        super.prepareForDeferredProcessing(message);

        origin = message.getOrigin().name().toLowerCase(Locale.ROOT);
        protocol = message.getProtocolVersion();
        remote = message.getRemote();
        method = message.getMethod();
        uri = message.getRequestUri();
        host = message.getHost();
        path = message.getPath();
        scheme = message.getScheme();
        port = message.getPort();
    }

    @Override
    protected void writeFieldValue(JsonGenerator generator) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("origin", origin);
        generator.writeStringField("type", "request");
        generator.writeStringField("protocol", protocol);
        generator.writeStringField("remote", remote);
        generator.writeStringField("method", method);
        generator.writeStringField("uri", uri);
        generator.writeStringField("host", host);
        generator.writeStringField("path", path);
        generator.writeStringField("scheme", scheme);
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
