package no.entur.logging.cloud.rr.grpc.filter;

import java.util.Map;

/**
 *
 * Per-key or per-map metadata filter
 *
 */

public interface GrpcMetadataFilter {

    Object filterMetadataKey(String key, Object value);

    Map<String, Object> filterMetadataKeys(Map<String, Object> metadata);

}
