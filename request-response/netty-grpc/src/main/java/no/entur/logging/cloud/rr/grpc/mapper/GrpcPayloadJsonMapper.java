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

    /**
     * Check whether mapping of a message can be deferred until the body is actually logged. A deferred message is
     * retained until then, so return false for messages which are better mapped right away, i.e. messages which are
     * too large to be logged: mapping them is cheap (the body only describes the message) and releases the message.
     *
     * @param m message
     * @return true if mapping can be deferred
     */

    default boolean isDeferrable(MessageOrBuilder m) {
        return true;
    }

}
