package no.entur.logging.cloud.rr.grpc.message;

import io.grpc.Status;

import java.util.Map;

public class GrpcResponse extends GrpcMessage {

    private final GrpcPayload payload;

    private final int number;

    private final long duration;

    private final Status.Code statusCode;
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
        this(headers, remote, uri, body == null ? null : GrpcPayload.of(body), origin, number, statusCode, duration);
    }

    /**
     * Constructor
     *
     * @param headers    map with headers, or null
     * @param remote     remote address, or null
     * @param uri        request uri or path
     * @param payload    payload or null
     * @param origin     remote (i.e. for incoming) or local (i.e. for outgoing)
     * @param number     response number
     * @param statusCode status code
     * @param duration   duration since first call
     */

    public GrpcResponse(Map<String, ?> headers, String remote, String uri, GrpcPayload payload, String origin, int number, Status.Code statusCode, long duration) {
        super(headers, remote, uri, "response", origin);
        this.payload = payload;
        this.number = number;
        this.statusCode = statusCode;
        this.duration = duration;
    }

    /**
     * Get the body, mapping it to JSON if not already mapped.
     *
     * @return body as raw JSON, or null
     */

    public String getBody() {
        if (payload == null) {
            return null;
        }
        return payload.getBody();
    }

    public GrpcPayload getPayload() {
        return payload;
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
}
