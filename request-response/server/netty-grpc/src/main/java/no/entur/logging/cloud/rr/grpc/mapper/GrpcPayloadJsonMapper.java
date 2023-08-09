package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import no.entur.logging.cloud.rr.grpc.filter.GrpcBodyFilter;

public interface GrpcPayloadJsonMapper {

    /**
     * Map gRPC message to a (JSON-serializable) Object.
     *
     * @param m message
     * @return mapping which can be directly appended to a json writer as a valid field value
     */

    String map(MessageOrBuilder m, GrpcBodyFilter filter) throws InvalidProtocolBufferException;

}
