package no.entur.logging.cloud.rr.grpc.filter;

public interface GrpcBodyFilter {

    @Deprecated
    String filterBody(String body);

    default CharSequence filterBody(CharSequence body) {
        return filterBody(body == null ? null : body.toString());
    }
}
