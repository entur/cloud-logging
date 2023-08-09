package no.entur.logging.cloud.rr.grpc.filter;

import java.util.Map;

public class NoneGrpcMetadataFilter implements GrpcMetadataFilter {

    private static final NoneGrpcMetadataFilter INSTANCE = new NoneGrpcMetadataFilter();

    public static NoneGrpcMetadataFilter getInstance() {
        return INSTANCE;
    }

    @Override
    public Object filterMetadataKey(String key, Object value) {
        return value;
    }

    @Override
    public Map<String, Object> filterMetadataKeys(Map<String, Object> metadata) {
        return metadata;
    }
}
