package no.entur.logging.cloud.rr.grpc.message;

import java.util.Map;

public class GrpcRequest extends GrpcMessage {

    private final String body;

    private final int number;

    private final long timeRemainingUntilDeadlineInMilliseconds;
    /**
     * Constructor
     *
     * @param headers map with headers, or null
     * @param remote  remote address, or null
     * @param uri     request uri or path
     * @param body    body or null
     * @param origin  remote (i.e. for incoming) or local (i.e. for outgoing)
     * @param number request number
     * @param timeRemainingUntilDeadlineInMilliseconds remaining time before deadline in milliseconds, or -1 if not available
     */

    public GrpcRequest(Map<String, ?> headers, String remote, String uri, String body, String origin, int number, long timeRemainingUntilDeadlineInMilliseconds) {
        super(headers, remote, uri, "request", origin);
        this.body = body;
        this.number = number;
        this.timeRemainingUntilDeadlineInMilliseconds = timeRemainingUntilDeadlineInMilliseconds;
    }

    public String getBody() {
        return body;
    }

    public int getNumber() {
        return number;
    }

    public long getTimeRemainingUntilDeadlineInMilliseconds() {
        return timeRemainingUntilDeadlineInMilliseconds;
    }
}
