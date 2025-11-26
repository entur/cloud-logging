package no.entur.logging.cloud.micrometer;

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
public class DevOpsLogbackMetrics extends io.micrometer.core.instrument.binder.logging.LogbackMetrics { // extend since there is no interface type
	private final LoggerContext loggerContext;
	private final Map<MeterRegistry, DevOpsMetricsTurboFilter> metricsTurboFilters = new ConcurrentHashMap<>();

	public DevOpsLogbackMetrics() {
		this((LoggerContext) LoggerFactory.getILoggerFactory());
	}

	public DevOpsLogbackMetrics(LoggerContext context) {
		this.loggerContext = context;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		DevOpsMetricsTurboFilter filter = createFilter(registry);
		metricsTurboFilters.put(registry, filter);
		loggerContext.addTurboFilter(filter);
	}

	protected DevOpsMetricsTurboFilter createFilter(MeterRegistry registry) {
		return new DevOpsMetricsTurboFilter(registry, emptyList());
	}

	@Override
	public void close() {
		for (DevOpsMetricsTurboFilter metricsTurboFilter : metricsTurboFilters.values()) {
			loggerContext.getTurboFilterList().remove(metricsTurboFilter);
		}
	}
}

