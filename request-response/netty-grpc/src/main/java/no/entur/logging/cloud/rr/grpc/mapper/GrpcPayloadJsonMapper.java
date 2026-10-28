package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import no.entur.logging.cloud.rr.grpc.filter.GrpcBodyFilter;
import no.entur.logging.cloud.rr.grpc.filter.GrpcPayloadFilter;

public interface GrpcPayloadJsonMapper {

    /**
     * Map gRPC message to a (JSON-serializable) Object.
     *
     * @param m message
     * @return mapping which can be directly appended to a json writer as a valid field value
     */

    String map(MessageOrBuilder m, GrpcBodyFilter filter) throws InvalidProtocolBufferException;

    /**
     * Map gRPC message to a (JSON-serializable) Object, filtering the payload while writing it.
     * <br><br>
     * The default implementation only supports {@linkplain GrpcPayloadFilter#NONE}, and fails for other payload filters,
     * so that payloads are never logged unfiltered.
     *
     * @param m message
     * @param payloadFilter filter applied while writing the message as JSON
     * @param bodyFilter filter applied to the written JSON
     * @return mapping which can be directly appended to a json writer as a valid field value, and whether it was truncated
     */

    default GrpcPayloadJson map(MessageOrBuilder m, GrpcPayloadFilter payloadFilter, GrpcBodyFilter bodyFilter) throws InvalidProtocolBufferException {
        if (!payloadFilter.isNone()) {
            throw new IllegalStateException(getClass().getName() + " does not support payload filters");
        }
        String json = map(m, bodyFilter);
        if (json == null) {
            return GrpcPayloadJson.NONE;
        }
        return new GrpcPayloadJson(json, false);
    }

}
