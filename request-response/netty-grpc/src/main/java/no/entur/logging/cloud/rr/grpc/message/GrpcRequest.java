package no.entur.logging.cloud.rr.grpc.message;

import java.util.Map;

public class GrpcRequest extends GrpcMessage {

    private final String body;

    private final int number;
    /**
     * Constructor
     *
     * @param headers map with headers, or null
     * @param remote  remote address, or null
     * @param uri     request uri or path
     * @param body    body or null
     * @param origin  remote (i.e. for incoming) or local (i.e. for outgoing)
     * @param number request number
     */

    public GrpcRequest(Map<String, ?> headers, String remote, String uri, String body, String origin, int number) {
        super(headers, remote, uri, "request", origin);
        this.body = body;
        this.number = number;
    }

    public String getBody() {
        return body;
    }

    public int getNumber() {
        return number;
    }
}
