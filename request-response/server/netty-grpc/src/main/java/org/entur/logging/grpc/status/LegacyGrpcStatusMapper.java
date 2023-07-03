package org.entur.logging.grpc.status;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import org.entur.logging.grpc.GrpcStatusDetailsMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated Use JsonPrinterStatusMapper
 */
@Deprecated
public class LegacyGrpcStatusMapper implements GrpcStatusMapper {

    private GrpcStatusDetailsMapper statusDetailsMapper;

    public LegacyGrpcStatusMapper(GrpcStatusDetailsMapper statusDetailsMapper) {
        this.statusDetailsMapper = statusDetailsMapper;
    }

    @Override
    public Object map(Status status) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < status.getDetailsCount(); i++) {
            Any detail = status.getDetails(i);

            try {
                Object mapping = statusDetailsMapper.map(detail);
                if (mapping != null) {
                    map.put(detail.getTypeUrl(), mapping);
                } else {
                    map.put(detail.getTypeUrl(), "[logging interceptor does not support this detail]");
                }

            } catch (InvalidProtocolBufferException e) {
                map.put(detail.getTypeUrl(), "[logging interceptor was unable to deserialize this detail]");
            }
        }
        return map;
    }
}
