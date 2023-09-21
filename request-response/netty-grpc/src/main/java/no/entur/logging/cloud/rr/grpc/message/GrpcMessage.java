package no.entur.logging.cloud.rr.grpc.message;

import java.util.Map;

public abstract class GrpcMessage {
    private static final long serialVersionUID = 1L;

    protected final Map<String, ?> headers;
    protected final String remote;
    protected final String uri;
    protected final String type;
    protected final String origin;

    /**
     *
     * Constructor
     *
     * @param headers map with headers, or null
     * @param remote remote address, or null
     * @param uri request uri or path
     * @param type type
     * @param origin origin; local or remote
     */

    public GrpcMessage(Map<String, ?> headers, String remote, String uri, String type, String origin) {
        this.headers = headers;
        this.remote = remote;
        this.uri = uri;
        this.type = type;
        this.origin = origin;
    }

    public Map<String, ?> getHeaders() {
        return headers;
    }

    public String getRemote() {
        return remote;
    }

    public String getUri() {
        return uri;
    }

    public String getType() {
        return type;
    }

    public String getOrigin() {
        return origin;
    }
}
