package no.entur.logging.cloud.gcp.micrometer;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;

/**
 * Logback driver with extra log levels. Originally copied from LogbackMetrics class in micrometer-core.
 * 
 */
@NonNullApi
@NonNullFields
public class StackdriverLogbackMetrics extends io.micrometer.core.instrument.binder.logging.LogbackMetrics { // extend since there is not interface type

	protected final LoggerContext loggerContext;
	protected final Map<MeterRegistry, StackdriverMetricsTurboFilter> metricsTurboFilters = new ConcurrentHashMap<>();

	public StackdriverLogbackMetrics() {
		this((LoggerContext) LoggerFactory.getILoggerFactory());
	}

	public StackdriverLogbackMetrics(LoggerContext context) {
		this.loggerContext = context;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		StackdriverMetricsTurboFilter filter = createFilter(registry);
		metricsTurboFilters.put(registry, filter);
		loggerContext.addTurboFilter(filter);
	}

	protected StackdriverMetricsTurboFilter createFilter(MeterRegistry registry) {
		return new StackdriverMetricsTurboFilter(registry, emptyList());
	}

	@Override
	public void close() {
		for (StackdriverMetricsTurboFilter metricsTurboFilter : metricsTurboFilters.values()) {
			loggerContext.getTurboFilterList().remove(metricsTurboFilter);
		}
	}
}

