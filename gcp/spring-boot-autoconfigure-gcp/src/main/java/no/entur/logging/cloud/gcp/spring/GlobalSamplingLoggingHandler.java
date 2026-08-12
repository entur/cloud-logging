package no.entur.logging.cloud.gcp.spring;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class GlobalSamplingLoggingHandler implements ObservationHandler<Observation.Context> {

    private static final Logger log = LoggerFactory.getLogger(GlobalSamplingLoggingHandler.class);

    @Override
    public void onStart(Observation.Context context) {
        // Retrieve the trace context populated by the Micrometer-OTel bridge
        TraceContext traceContext = context.get(TraceContext.class);
        
        if (traceContext != null) {
            if(traceContext.sampled()) {
                MDC.put("traceSampled", "true");
            }
        }
    }

    @Override
    public void onScopeClosed(Observation.Context context) {
        MDC.remove("traceSampled");
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        // Apply globally across all types of operations (HTTP requests, tasks, scheduled jobs, etc.)
        return true;
    }
}