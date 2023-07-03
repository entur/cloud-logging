package org.entur.logging.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

public interface GrpcStatusDetailsMapper {

    /**
     * Map detail to a (JSON-serializable) Map.
     *
     * @param detail input detail
     * @return mapping
     * @throws InvalidProtocolBufferException if unexpectedly unable to decode a known type
     */

    Object map(Any detail) throws InvalidProtocolBufferException;

}
