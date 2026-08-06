package no.entur.logging.cloud.gcp.trace.spring.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrderedTraceIdGrpcMdcContextServerInterceptorTest {

    private final OrderedTraceIdGrpcMdcContextServerInterceptor interceptor = new OrderedTraceIdGrpcMdcContextServerInterceptor(0);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void interceptCall_noRealTrace_fallsBackToCorrelationId() {
        Map<String, String> context = new HashMap<>();
        context.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY, "my-correlation-id");
        GrpcMdcContext grpcMdcContext = new GrpcMdcContext(context);

        runInterceptCall(grpcMdcContext);

        assertThat(grpcMdcContext.get(OrderedTraceIdGrpcMdcContextServerInterceptor.TRACE_MDC_KEY))
                .isEqualTo("my-correlation-id");
        assertThat(grpcMdcContext.containsKey(OrderedTraceIdGrpcMdcContextServerInterceptor.SPAN_ID_MDC_KEY)).isTrue();
    }

    @Test
    void interceptCall_realTraceIdInMdc_defersAndDoesNotSetFallback() {
        // a real trace from OpenTelemetry always carries both trace_id and span_id together
        MDC.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("span_id", "00f067aa0ba902b7");

        Map<String, String> context = new HashMap<>();
        context.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY, "my-correlation-id");
        GrpcMdcContext grpcMdcContext = new GrpcMdcContext(context);

        runInterceptCall(grpcMdcContext);

        assertThat(grpcMdcContext.containsKey(OrderedTraceIdGrpcMdcContextServerInterceptor.TRACE_MDC_KEY)).isFalse();
        assertThat(grpcMdcContext.containsKey(OrderedTraceIdGrpcMdcContextServerInterceptor.SPAN_ID_MDC_KEY)).isFalse();
    }

    @Test
    void interceptCall_realCamelCaseTraceIdInMdc_defersAndDoesNotSetFallback() {
        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("spanId", "00f067aa0ba902b7");

        Map<String, String> context = new HashMap<>();
        context.put(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY, "my-correlation-id");
        GrpcMdcContext grpcMdcContext = new GrpcMdcContext(context);

        runInterceptCall(grpcMdcContext);

        assertThat(grpcMdcContext.containsKey(OrderedTraceIdGrpcMdcContextServerInterceptor.TRACE_MDC_KEY)).isFalse();
        assertThat(grpcMdcContext.containsKey(OrderedTraceIdGrpcMdcContextServerInterceptor.SPAN_ID_MDC_KEY)).isFalse();
    }

    @Test
    void interceptCall_noGrpcMdcContext_doesNotThrow() {
        ServerCall<Object, Object> call = mock(ServerCall.class);
        Metadata headers = new Metadata();
        ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);

        interceptor.interceptCall(call, headers, next);
        // no context present outside GrpcMdcContext.runInNewContext - just verifying no exception is thrown
    }

    @SuppressWarnings("unchecked")
    private void runInterceptCall(GrpcMdcContext grpcMdcContext) {
        ServerCall<Object, Object> call = mock(ServerCall.class);
        Metadata headers = new Metadata();
        ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
        ServerCall.Listener<Object> listener = mock(ServerCall.Listener.class);
        when(next.startCall(call, headers)).thenReturn(listener);

        AtomicReference<ServerCall.Listener<Object>> result = new AtomicReference<>();
        GrpcMdcContext.runInNewContext(grpcMdcContext, () ->
                result.set(interceptor.interceptCall(call, headers, next)));

        assertThat(result.get()).isSameInstanceAs(listener);
    }
}
