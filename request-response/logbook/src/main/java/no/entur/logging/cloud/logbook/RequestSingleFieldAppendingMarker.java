package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonGenerator;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

public class RequestSingleFieldAppendingMarker extends AbstractSingleFieldAppendingMarker<HttpRequest> {

    public static final String MARKER_NAME = RequestSingleFieldAppendingMarker.MARKER_NAME_PREFIX + "REQUEST";

    protected String origin;
    protected String protocol;
    protected String remote;
    protected String method;
    protected String uri;
    protected String host;
    protected String path;
    protected String scheme;
    protected Optional<Integer> port;

    public RequestSingleFieldAppendingMarker(HttpRequest request, String body, boolean wellformed) {
        super(MARKER_NAME, request, body, wellformed);
    }

    @Override
    protected void prepareForDeferredProcessing(HttpRequest message) {
        super.prepareForDeferredProcessing(message);

        Origin origin = message.getOrigin();
        if(origin != null) {
            this.origin = origin.name().toLowerCase(Locale.ROOT);
        }
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

        if(origin != null) {
            generator.writeStringField("origin", origin);
        }
        generator.writeStringField("type", "request");
        if(protocol != null) {
            generator.writeStringField("protocol", protocol);
        }
        if(remote != null) {
            generator.writeStringField("remote", remote);
        }
        if(method != null) {
            generator.writeStringField("method", method);
        }
        if(uri != null) {
            generator.writeStringField("uri", uri);
        }
        if(host != null) {
            generator.writeStringField("host", host);
        }
        if(path != null) {
            generator.writeStringField("path", path);
        }
        if(scheme != null) {
            generator.writeStringField("scheme", scheme);
        }
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
