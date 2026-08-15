package no.entur.logging.cloud.gcp.spring;

import io.micrometer.tracing.Tracer;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverOpenTelemetryTraceMdcJsonProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Autoconfiguration that registers {@link TraceSampledMdcHandler} when:
 * <ul>
 *   <li>{@code io.micrometer:micrometer-tracing} is on the classpath ({@code Tracer} class present), and</li>
 *   <li>the OpenTelemetry Java agent is <em>not</em> attached.</li>
 * </ul>
 * Both conditions are evaluated at class level so that the entire configuration is skipped when
 * either condition is not met.
 *
 * <p>When the OTel agent is used, the agent writes trace context to MDC using its own keys,
 * handled by {@code StackdriverOpenTelemetryTraceMdcJsonProvider}. In that case this
 * configuration must not interfere.
 *
 * <p>{@code @AutoConfigureAfter} on the tracing autoconfiguration name ensures that the
 * {@link Tracer} bean is available for injection when this configuration is processed.
 */
@AutoConfiguration(afterName = "org.springframework.boot.micrometer.tracing.autoconfigure.TracingAutoConfiguration")
@ConditionalOnClass(Tracer.class)
@Conditional(GcpMicrometerTraceAutoConfiguration.NoOpenTelemetryAgentCondition.class)
public class GcpMicrometerTraceAutoConfiguration {

    /**
     * Condition that matches when the OpenTelemetry Java agent is <em>not</em> present.
     */
    static class NoOpenTelemetryAgentCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !StackdriverOpenTelemetryTraceMdcJsonProvider.isOtelAgent();
        }
    }

    /**
     * Registers {@link TraceSampledMdcHandler}.
     *
     * <p>{@link ObjectProvider} is used so that the bean is a graceful no-op when
     * micrometer-tracing is on the classpath but no {@link Tracer} bean exists
     * (e.g. when {@code management.tracing.enabled=false}).
     */
    @Bean
    public TraceSampledMdcHandler traceSampledMdcHandler(ObjectProvider<Tracer> tracerProvider) {
        return new TraceSampledMdcHandler(tracerProvider.getIfAvailable());
    }
}
