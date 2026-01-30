package no.entur.logging.cloud.logbook.ondemand;

import tools.jackson.core.JsonGenerator;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

public class RequestOndemandSingleFieldAppendingMarker extends AbstractOndemandSingleFieldAppendingMarker<HttpRequest> {

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

    public RequestOndemandSingleFieldAppendingMarker(HttpRequest request, HttpMessageBodyWriter httpMessageBodyWriter) {
        super(MARKER_NAME, request, httpMessageBodyWriter);
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

        generator.writeStringProperty("origin", origin);
        generator.writeStringProperty("type", "request");
        generator.writeStringProperty("protocol", protocol);
        generator.writeStringProperty("remote", remote);
        generator.writeStringProperty("method", method);
        generator.writeStringProperty("uri", uri);
        generator.writeStringProperty("host", host);
        generator.writeStringProperty("path", path);
        generator.writeStringProperty("scheme", scheme);
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
