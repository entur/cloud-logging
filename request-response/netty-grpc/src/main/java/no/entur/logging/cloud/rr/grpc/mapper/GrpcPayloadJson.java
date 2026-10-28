package no.entur.logging.cloud.rr.grpc.mapper;

/**
 * A gRPC payload written as JSON.
 */

public class GrpcPayloadJson {

    public static final GrpcPayloadJson NONE = new GrpcPayloadJson(null, false);

    private final String json;

    private final boolean truncated;

    /**
     * Constructor
     *
     * @param json      JSON which can be directly appended to a json writer as a valid field value, or null
     * @param truncated whether the payload was truncated, i.e. because of max size
     */

    public GrpcPayloadJson(String json, boolean truncated) {
        this.json = json;
        this.truncated = truncated;
    }

    public String getJson() {
        return json;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
