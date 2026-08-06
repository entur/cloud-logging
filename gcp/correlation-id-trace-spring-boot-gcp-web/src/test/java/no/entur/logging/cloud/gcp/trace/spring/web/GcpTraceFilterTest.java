package no.entur.logging.cloud.gcp.trace.spring.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import no.entur.logging.cloud.trace.spring.web.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GcpTraceFilterTest {

    private final GcpTraceFilter filter = new GcpTraceFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void doFilter_noRealTraceNoCorrelationId_fallsBackToRandomTrace() throws Exception {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        AtomicReference<String> traceDuringCall = new AtomicReference<>();
        AtomicReference<String> spanIdDuringCall = new AtomicReference<>();
        doAnswer(invocation -> {
            traceDuringCall.set(MDC.get(GcpTraceFilter.TRACE_MDC_KEY));
            spanIdDuringCall.set(MDC.get(GcpTraceFilter.SPAN_ID_MDC_KEY));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(traceDuringCall.get()).isNotNull();
        assertThat(spanIdDuringCall.get()).isNotNull();
        // cleaned up again once the request has been handled
        assertThat(MDC.get(GcpTraceFilter.TRACE_MDC_KEY)).isNull();
        assertThat(MDC.get(GcpTraceFilter.SPAN_ID_MDC_KEY)).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_correlationIdPresent_usesCorrelationIdAsTrace() throws Exception {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getAttribute(CorrelationIdFilter.CORRELACTION_ID_HTTP_REQUEST_KEY))
                .thenReturn("my-correlation-id");

        AtomicReference<String> traceDuringCall = new AtomicReference<>();
        doAnswer(invocation -> {
            traceDuringCall.set(MDC.get(GcpTraceFilter.TRACE_MDC_KEY));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(traceDuringCall.get()).isEqualTo("my-correlation-id");
    }

    @Test
    void doFilter_realTraceIdInMdc_defersAndDoesNotOverwrite() throws Exception {
        MDC.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");

        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(MDC.get(GcpTraceFilter.TRACE_MDC_KEY)).isNull();
        assertThat(MDC.get(GcpTraceFilter.SPAN_ID_MDC_KEY)).isNull();
        // the real trace id set by OpenTelemetry instrumentation is left untouched, so
        // StackdriverOpenTelemetryTraceMdcJsonProvider can derive the correctly-prefixed value from it
        assertThat(MDC.get("trace_id")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void doFilter_realCamelCaseTraceIdInMdc_defers() throws Exception {
        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");

        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(MDC.get(GcpTraceFilter.TRACE_MDC_KEY)).isNull();
    }
}
