package no.entur.logging.cloud.rr.grpc;

import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class CorrelationIdGrpcMdcContextTest {

    @Test
    public void test() {
        CorrelationIdGrpcMdcContext.newContext().build();
    }

    public Boolean call() throws Exception {
        return CorrelationIdGrpcMdcContext.newContext().build().call(() -> false);
    }

    @Test
    public void testForwardToExecutor() throws InterruptedException {
        CorrelationIdGrpcMdcContext grpcMdcContext = CorrelationIdGrpcMdcContext
                .newContext()
                .withField("key", "value")
                .build();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            grpcMdcContext.run(() -> {
                CorrelationIdGrpcMdcContext clone = CorrelationIdGrpcMdcContext.newContext()
                        .withField("extraKey", "extraValue")
                        .build();

                executorService.submit(clone.run(() -> {
                    GrpcMdcContext cloneInContext = GrpcMdcContext.get();

                    assertEquals(cloneInContext.get("extraKey"), "extraValue");
                    assertEquals(cloneInContext.get("key"), "value");

                    assertEquals(grpcMdcContext.getCorrelationId(), cloneInContext.get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY));
                    assertEquals(grpcMdcContext.getRequestId(), cloneInContext.get(CorrelationIdGrpcMdcContext.REQUEST_ID_MDC_KEY));
                }));
            });
        } finally {
            executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
        }
    }
}
