package org.entur.logging.grpc;

import io.grpc.Metadata;

import java.util.Map;

public interface MetadataJsonMapper {

    Map<String, Object> toJson(final Metadata headers);

}