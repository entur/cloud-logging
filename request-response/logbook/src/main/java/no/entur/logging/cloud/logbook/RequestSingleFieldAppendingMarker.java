package no.entur.logging.cloud.logbook;

import tools.jackson.core.JsonGenerator;
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
    protected void writeFieldValue(JsonGenerator generator) {
        generator.writeStartObject();

        if(origin != null) {
            generator.writeStringProperty("origin", origin);
        }
        generator.writeStringProperty("type", "request");
        if(protocol != null) {
            generator.writeStringProperty("protocol", protocol);
        }
        if(remote != null) {
            generator.writeStringProperty("remote", remote);
        }
        if(method != null) {
            generator.writeStringProperty("method", method);
        }
        if(uri != null) {
            generator.writeStringProperty("uri", uri);
        }
        if(host != null) {
            generator.writeStringProperty("host", host);
        }
        if(path != null) {
            generator.writeStringProperty("path", path);
        }
        if(scheme != null) {
            generator.writeStringProperty("scheme", scheme);
        }
        if(port.isEmpty()) {
            generator.writeNumberProperty("port", 80);
        } else {
            generator.writeNumberProperty("port", port.get());
        }

        writeHeaders(generator);
        writeBody(generator);

        generator.writeEndObject();
    }
}
