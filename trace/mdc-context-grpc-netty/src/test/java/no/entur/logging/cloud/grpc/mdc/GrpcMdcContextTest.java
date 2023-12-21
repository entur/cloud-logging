package no.entur.logging.cloud.grpc.mdc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class GrpcMdcContextTest {

    @Test
    public void testBuilder() {
        GrpcMdcContext grpcMdcContext = GrpcMdcContext
                .newContext()
                .withField("key", "value")
                .build();

        assertEquals(grpcMdcContext.get("key"), "value");
    }

    @Test
    public void testForwardToExecutor() throws InterruptedException {
        GrpcMdcContext grpcMdcContext = GrpcMdcContext
                .newContext()
                .withField("key", "value")
                .build();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            grpcMdcContext.run(() -> {
                GrpcMdcContext clone = GrpcMdcContext.newContext()
                        .withField("extraKey", "extraValue")
                        .build();

                executorService.submit(clone.run(() -> {
                    GrpcMdcContext cloneInContext = GrpcMdcContext.get();

                    assertEquals(cloneInContext.get("extraKey"), "extraValue");
                    assertEquals(cloneInContext.get("key"), "value");

                }));
            });
        } finally {
            executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
        }
    }


    public Boolean wrap() throws Exception {
        return GrpcMdcContext.newContext().build().call(() -> Boolean.FALSE);
    }
}
