package no.entur.logging.cloud.rr.grpc.message;

import io.grpc.Status;

import java.util.Map;

public class GrpcResponse extends GrpcMessage {

    private final String body;

    private final int number;

    private final long duration;

    private final Status.Code statusCode;

    private final boolean truncated;

    /**
     * Constructor
     *
     * @param headers    map with headers, or null
     * @param remote     remote address, or null
     * @param uri        request uri or path
     * @param body       body or null
     * @param origin     remote (i.e. for incoming) or local (i.e. for outgoing)
     * @param number     response number
     * @param statusCode status code
     * @param duration   duration since first call
     */

    public GrpcResponse(Map<String, ?> headers, String remote, String uri, String body, String origin, int number, Status.Code statusCode, long duration) {
        this(headers, remote, uri, body, origin, number, statusCode, duration, false);
    }

    /**
     * Constructor
     *
     * @param headers    map with headers, or null
     * @param remote     remote address, or null
     * @param uri        request uri or path
     * @param body       body or null
     * @param origin     remote (i.e. for incoming) or local (i.e. for outgoing)
     * @param number     response number
     * @param statusCode status code
     * @param duration   duration since first call
     * @param truncated  whether the body was truncated
     */

    public GrpcResponse(Map<String, ?> headers, String remote, String uri, String body, String origin, int number, Status.Code statusCode, long duration, boolean truncated) {
        super(headers, remote, uri, "response", origin);
        this.body = body;
        this.number = number;
        this.statusCode = statusCode;
        this.duration = duration;
        this.truncated = truncated;
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

    public long getDuration() {
        return duration;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
