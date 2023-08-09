package no.entur.logging.cloud.rr.grpc.filter;

import java.util.Map;

/**
 *
 * Default filter, masking authorization field
 *
 */
public class DefaultGrpcMetadataFilter implements GrpcMetadataFilter {

    private static final DefaultGrpcMetadataFilter INSTANCE = new DefaultGrpcMetadataFilter();

    public static DefaultGrpcMetadataFilter getInstance() {
        return INSTANCE;
    }

    @Override
    public Object filterMetadataKey(String key, Object value) {
        if(key.equals("authorization")) {
            return FilterUtils.toSHA(value.toString());
        }
        return value;
    }

    @Override
    public Map<String, Object> filterMetadataKeys(Map<String, Object> metadata) {
        return metadata;
    }
}
