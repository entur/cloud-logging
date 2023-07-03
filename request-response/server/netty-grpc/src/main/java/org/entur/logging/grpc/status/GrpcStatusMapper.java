package org.entur.logging.grpc.status;

import com.google.protobuf.InvalidProtocolBufferException;

public interface GrpcStatusMapper {

    /**
     * Map grpc status to a (JSON-serializable) Object.
     *
     * @param status input status
     * @return mapping
     */

    Object map(com.google.rpc.Status status);

}
