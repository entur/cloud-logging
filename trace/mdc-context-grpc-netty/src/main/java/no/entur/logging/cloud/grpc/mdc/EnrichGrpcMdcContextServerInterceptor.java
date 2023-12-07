package no.entur.logging.cloud.grpc.mdc;

import io.grpc.Metadata;
import io.grpc.ServerCall;

@FunctionalInterface
public interface EnrichGrpcMdcContextServerInterceptor {

    void enrich(ServerCall serverCall, Metadata metadata, GrpcMdcContext grpcMdcContext);
}
