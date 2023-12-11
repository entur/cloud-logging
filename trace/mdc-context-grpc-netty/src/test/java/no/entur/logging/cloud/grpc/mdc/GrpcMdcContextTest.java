package no.entur.logging.cloud.grpc.mdc;

import org.junit.jupiter.api.Test;

public class GrpcMdcContextTest {

    @Test
    public void test() {
        GrpcMdcContext grpcMdcContext = GrpcMdcContext.newContext().build();


    }

    public Boolean wrap() throws Exception {
        return GrpcMdcContext.newContext().build().call(() -> Boolean.FALSE);
    }
}
