package no.entur.logging.cloud.rr.grpc.mapper;

public interface GrpcStatusMapper {

    /**
     * Map grpc status to a (JSON-serializable) Object.
     *
     * @param status input status
     * @return mapping; use {@linkplain tools.jackson.databind.util.RawValue} for pre-serialized JSON
     */

    Object map(com.google.rpc.Status status);

}
