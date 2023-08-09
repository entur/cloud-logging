package no.entur.logging.cloud.rr.grpc.message;

import java.util.Map;

public class GrpcDisconnect extends GrpcMessage {

    protected final int requestCount;
    protected final int responseCount;

    protected final long totalPayloadSize;

    protected final long duration;

    /**
     * Constructor
     *
     * @param headers          map with headers, or null
     * @param remote           remote address, or null
     * @param uri              request uri or path
     * @param origin           origin; local or remote
     * @param requestCount     number of request
     * @param responseCount    number of responses
     * @param totalPayloadSize
     * @param duration
     */
    public GrpcDisconnect(Map<String, ?> headers, String remote, String uri, String origin, int requestCount, int responseCount, long totalPayloadSize, long duration) {
        super(headers, remote, uri, "disconnect", origin);
        this.requestCount = requestCount;
        this.responseCount = responseCount;
        this.totalPayloadSize = totalPayloadSize;
        this.duration = duration;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public long getTotalPayloadSize() {
        return totalPayloadSize;
    }

    public long getDuration() {
        return duration;
    }
}
