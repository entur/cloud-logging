package no.entur.logging.cloud.grpc.mdc;

import no.entur.logging.cloud.appender.MdcContributer;

import java.util.Collections;
import java.util.Map;

public class GrpcMdcContributer implements MdcContributer {

    @Override
    public Map<String, String> getMdc() {
        GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
        if(grpcMdcContext != null) {
            return grpcMdcContext.getContext();
        }
        return Collections.emptyMap();
    }
}
