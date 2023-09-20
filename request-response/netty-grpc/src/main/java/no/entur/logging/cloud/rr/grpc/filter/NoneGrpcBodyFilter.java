package no.entur.logging.cloud.rr.grpc.filter;

public class NoneGrpcBodyFilter implements GrpcBodyFilter {

    private static final NoneGrpcBodyFilter INSTANCE = new NoneGrpcBodyFilter();

    public static NoneGrpcBodyFilter getInstance() {
        return INSTANCE;
    }

    @Override
    public String filterBody(String body) {
        return body;
    }
}
