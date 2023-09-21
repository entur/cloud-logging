package no.entur.logging.cloud.rr.grpc.filter;

public interface GrpcBodyFilter {

    String filterBody(String body);

}
