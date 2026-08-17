package no.entur.logging.cloud.gcp.spring;

import no.entur.logging.cloud.gcp.logback.logstash.StackdriverOpenTelemetryTraceMdcJsonProvider;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches when the OpenTelemetry Java agent is <em>not</em> present.
 */
public class NoOpenTelemetryAgentCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !StackdriverOpenTelemetryTraceMdcJsonProvider.isOtelAgent();
    }
}
