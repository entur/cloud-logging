package org.entur.example.web.otel.agent;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TraceSampledMdcHandler implements ObservationHandler<Observation.Context> {

    private static final String TRACE_SAMPLED_MDC_KEY = "traceSampled";
    private final Tracer tracer;

    public TraceSampledMdcHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onScopeOpened(Observation.Context context) {
        System.out.println("OPENED");

        // 1. Get the current vendor-agnostic trace context
        TraceContext traceContext = this.tracer.currentTraceContext().context();

        if (traceContext != null && traceContext.sampled()) {
            MDC.put(TRACE_SAMPLED_MDC_KEY, "true");
        }
    }

    @Override
    public void onScopeClosed(Observation.Context context) {
        MDC.remove(TRACE_SAMPLED_MDC_KEY);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }
}