package no.entur.logging.cloud.azure.micrometer;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;

/**
 * Logback driver with extra log levels. Originally copied from LogbackMetrics class in micrometer-core.
 * 
 */
@org.jspecify.annotations.NullMarked
public class AzureLogbackMetrics extends io.micrometer.core.instrument.binder.logging.LogbackMetrics { // extend since there is not interface type
	private final LoggerContext loggerContext;
	private final Map<MeterRegistry, AzureMetricsTurboFilter> metricsTurboFilters = new ConcurrentHashMap<>();

	public AzureLogbackMetrics() {
		this((LoggerContext) LoggerFactory.getILoggerFactory());
	}

	public AzureLogbackMetrics(LoggerContext context) {
		this.loggerContext = context;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		AzureMetricsTurboFilter filter = new AzureMetricsTurboFilter(registry, emptyList());
		metricsTurboFilters.put(registry, filter);
		loggerContext.addTurboFilter(filter);
	}

	@Override
	public void close() {
		for (AzureMetricsTurboFilter metricsTurboFilter : metricsTurboFilters.values()) {
			loggerContext.getTurboFilterList().remove(metricsTurboFilter);
		}
	}
}

