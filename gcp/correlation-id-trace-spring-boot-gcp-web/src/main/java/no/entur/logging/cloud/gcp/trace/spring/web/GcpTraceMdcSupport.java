package no.entur.logging.cloud.gcp.trace.spring.web;

import no.entur.logging.cloud.trace.spring.web.CorrelationIdFilter;
import org.slf4j.MDC;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Helper for adding correlation id into the SLF4J MDC.
 *
 */

public class GcpTraceMdcSupport implements Closeable {

    public static final String REQUEST_ID_MDC_KEY = CorrelationIdFilter.REQUEST_ID_MDC_KEY;
    public static final String CORRELATION_ID_MDC_KEY = CorrelationIdFilter.CORRELATION_ID_MDC_KEY;

    public static final String TRACE_MDC_KEY = GcpTraceFilter.TRACE_MDC_KEY;
    public static final String SPAN_ID_MDC_KEY = GcpTraceFilter.SPAN_ID_MDC_KEY;

    public static GcpTraceMdcSupportBuilder newBuilder() {
        return new GcpTraceMdcSupportBuilder();
    }

    @FunctionalInterface
    public static interface Consumer {
        void consume(String correlationId, String requestId, String spanId, String trace);
    }

    public static String currentCorrelationId() {
        return MDC.get(GcpTraceMdcSupport.CORRELATION_ID_MDC_KEY);
    }

    public static String currentTrace() {
        return MDC.get(GcpTraceMdcSupport.TRACE_MDC_KEY);
    }

    public static String currentSpanId() {
        return MDC.get(GcpTraceMdcSupport.SPAN_ID_MDC_KEY);
    }

    public static String currentRequestId() {
        return MDC.get(GcpTraceMdcSupport.REQUEST_ID_MDC_KEY);
    }

    public static void get(Consumer wrap) {
        wrap.consume(currentCorrelationId(), currentRequestId(), currentSpanId(), currentTrace());
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

        String spanId = currentSpanId();
        if(spanId == null) {
            ThreadLocalRandom current = ThreadLocalRandom.current();
            long random = current.nextLong();
            spanId = Long.toHexString(random);
        }

        String trace = currentTrace();
        if(trace == null) {
            trace = correlationId;
        }

        wrap.consume(correlationId, requestId, spanId, trace);
    }


    private final String correlationId;
    private final String requestId;
    private final String trace;
    private final String spanId;

    protected GcpTraceMdcSupport(String correlationId, String requestId, String trace, String spanId) {
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.trace = trace;
        this.spanId = spanId;
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
        MDC.remove(SPAN_ID_MDC_KEY);
        MDC.remove(TRACE_MDC_KEY);
    }

    public void run(Runnable r) {
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(SPAN_ID_MDC_KEY, spanId);
        MDC.put(TRACE_MDC_KEY, trace);
        try {
            r.run();
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove(SPAN_ID_MDC_KEY);
            MDC.remove(TRACE_MDC_KEY);
        }
    }

    public <T> T call(Callable<T> r) throws Exception {
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(SPAN_ID_MDC_KEY, spanId);
        MDC.put(TRACE_MDC_KEY, trace);
        try {
            return r.call();
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove(SPAN_ID_MDC_KEY);
            MDC.remove(TRACE_MDC_KEY);
        }
    }

    @Override
    public void close() {
        clear();
    }
}
