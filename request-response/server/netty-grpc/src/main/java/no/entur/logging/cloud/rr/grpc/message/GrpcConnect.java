package no.entur.logging.cloud.rr.grpc.message;

import java.util.Map;

public class GrpcConnect extends GrpcMessage {

    /**
     * Constructor
     *
     * @param headers map with headers, or null
     * @param remote  remote address, or null
     * @param uri     request uri or path
     * @param origin  origin; local or remote
     */
    public GrpcConnect(Map<String, ?> headers, String remote, String uri, String origin) {
        super(headers, remote, uri, "connect", origin);
    }
}
