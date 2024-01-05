package no.entur.logging.cloud.trace.spring.web;

import org.slf4j.MDC;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 *
 * Helper for adding correlation id into the SLF4J MDC.
 *
 */

public class CorrelationIdMdcSupport implements Closeable {

    public static final String REQUEST_ID_MDC_KEY = CorrelationIdFilter.REQUEST_ID_MDC_KEY;
    public static final String CORRELATION_ID_MDC_KEY = CorrelationIdFilter.CORRELATION_ID_MDC_KEY;

    public static CorrelationIdMdcSupportBuilder newBuilder() {
        return new CorrelationIdMdcSupportBuilder();
    }

    @FunctionalInterface
    public static interface Consumer {
        void consume(String correlationId, String requestId);
    }

    public static String currentCorrelationId() {
        return MDC.get(CorrelationIdMdcSupport.CORRELATION_ID_MDC_KEY);
    }

    public static String currentRequestId() {
        return MDC.get(CorrelationIdMdcSupport.REQUEST_ID_MDC_KEY);
    }

    public static void get(Consumer wrap) {
        wrap.consume(currentCorrelationId(), currentRequestId());
    }

    public static void getOrCreate(Consumer wrap) {
        String correlationId = currentCorrelationId();
        if(correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        String requestId = currentRequestId();
        if(requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        wrap.consume(correlationId, requestId);
    }


    private final String correlationId;
    private final String requestId;

    protected CorrelationIdMdcSupport(String correlationId, String requestId) {
        this.correlationId = correlationId;
        this.requestId = requestId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void clear() {
        MDC.remove(REQUEST_ID_MDC_KEY);
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }

    public void run(Runnable r) {
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        try {
            r.run();
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    public <T> T call(Callable<T> r) throws Exception {
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        try {
            return r.call();
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    @Override
    public void close() {
        clear();
    }
}
