package no.entur.logging.cloud.rr.grpc.filter;

public interface GrpcBodyFilter {

    @Deprecated
    String filterBody(String body);

    CharSequence filterBody(CharSequence body);
}
