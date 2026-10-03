package no.entur.logging.cloud.rr.grpc.mapper;

/**
 * Pre-serialized JSON value, written as-is (i.e. as structured JSON, not as an escaped string).
 * The JSON must be well-formed.
 */

public class RawJson {

    private final String json;

    public RawJson(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    @Override
    public String toString() {
        return json;
    }
}
