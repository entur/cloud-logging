package no.entur.logging.cloud.rr.grpc.message;

import io.grpc.Status;

import java.util.Map;

public class GrpcResponse extends GrpcMessage {

    private final String body;

    private final int number;

    private final Status.Code statusCode;
    /**
     * Constructor
     *
     * @param headers map with headers, or null
     * @param remote  remote address, or null
     * @param uri     request uri or path
     * @param body    body or null
     * @param origin  remote (i.e. for incoming) or local (i.e. for outgoing)
     * @param number  response number
     * @param statusCode    status code
     */

    public GrpcResponse(Map<String, ?> headers, String remote, String uri, String body, String origin, int number, Status.Code statusCode) {
        super(headers, remote, uri, "response", origin);
        this.body = body;
        this.number = number;
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public int getNumber() {
        return number;
    }

    public Status.Code getStatusCode() {
        return statusCode;
    }
}
