package no.entur.logging.cloud.rr.grpc.mapper;

import io.grpc.Metadata;
import no.entur.logging.cloud.rr.grpc.filter.GrpcMetadataFilter;

import java.util.Map;

public interface GrpcMetadataJsonMapper {

    Map<String, Object> map(final Metadata headers, GrpcMetadataFilter filter);

}