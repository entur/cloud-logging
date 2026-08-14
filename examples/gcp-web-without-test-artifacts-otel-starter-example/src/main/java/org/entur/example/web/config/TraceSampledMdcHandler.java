package org.entur.example.web.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.TraceContext;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverMicrometerTraceMdcJsonProvider;
import org.slf4j.MDC;

/**
 * Adds the trace-sampled flag to the SLF4J MDC so that
 * {@link StackdriverMicrometerTraceMdcJsonProvider} can map it to the
 * {@code logging.googleapis.com/trace_sampled} JSON field recognised by GCP Cloud Logging.
 *
 * <p>Register this as a Spring bean only when:
 * <ul>
 *   <li>a {@link Tracer} bean exists (i.e. micrometer-tracing / Spring Boot OTel starter is enabled), and</li>
 *   <li>the OpenTelemetry Java agent is <em>not</em> attached (the agent provides its own MDC keys).</li>
 * </ul>
 * See {@link LogConfiguration} for conditional bean registration.
 */
public class TraceSampledMdcHandler implements ObservationHandler<Observation.Context> {

    private final Tracer tracer;

    public TraceSampledMdcHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onScopeOpened(Observation.Context context) {
        if (tracer == null) return;
        TraceContext traceContext = this.tracer.currentTraceContext().context();
        if (traceContext != null && Boolean.TRUE.equals(traceContext.sampled())) {
            MDC.put(StackdriverMicrometerTraceMdcJsonProvider.MICROMETER_SAMPLED_KEY, "true");
        }
    }

    @Override
    public void onScopeClosed(Observation.Context context) {
        MDC.remove(StackdriverMicrometerTraceMdcJsonProvider.MICROMETER_SAMPLED_KEY);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }
}
