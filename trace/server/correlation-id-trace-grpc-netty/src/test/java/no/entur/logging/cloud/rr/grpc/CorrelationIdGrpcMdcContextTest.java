package no.entur.logging.cloud.rr.grpc;

import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContext;
import org.junit.jupiter.api.Test;

public class CorrelationIdGrpcMdcContextTest {

    @Test
    public void test() {
        CorrelationIdGrpcMdcContext.newContext().build();
    }

    public Boolean call() throws Exception {
        return CorrelationIdGrpcMdcContext.newContext().build().call(() -> false);
    }
}
