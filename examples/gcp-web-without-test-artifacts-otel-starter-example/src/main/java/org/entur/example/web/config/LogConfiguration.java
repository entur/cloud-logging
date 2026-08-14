package org.entur.example.web.config;

import io.micrometer.tracing.Tracer;
import java.util.HashSet;
import java.util.Set;

import no.entur.logging.cloud.gcp.trace.spring.web.NoOpenTelemetryAgentCondition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.json.JsonBodyFilters;

@Configuration
public class LogConfiguration {

	@Bean
	public BodyFilter filterBody() {
        final Set<String> properties = new HashSet<>();
        properties.add("secret");
        return JsonBodyFilters.replaceJsonStringProperty(properties, "hidden");
	}

	/**
	 * Registers {@link TraceSampledMdcHandler} when the OpenTelemetry Java agent is not
	 * present. When the OTel agent is used, trace context is written to MDC by the agent
	 * itself using different keys handled by {@code StackdriverOpenTelemetryTraceMdcJsonProvider}.
	 *
	 * <p>{@link ObjectProvider} is used so that the bean is a graceful no-op when
	 * micrometer-tracing is on the classpath but no {@link Tracer} bean exists
	 * (e.g. when {@code management.tracing.enabled=false}).
	 */
	@Bean
	@Conditional(NoOpenTelemetryAgentCondition.class)
	public TraceSampledMdcHandler traceSampledMdcHandler(ObjectProvider<Tracer> tracerProvider) {
		return new TraceSampledMdcHandler(tracerProvider.getIfAvailable());
	}
	
}

