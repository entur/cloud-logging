package no.entur.logging.cloud.gcp.micrometer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

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
	static ThreadLocal<Boolean> ignoreMetrics = new ThreadLocal<>();

	private final LoggerContext loggerContext;
	private final Map<MeterRegistry, StackdriverMetricsTurboFilter> metricsTurboFilters = new ConcurrentHashMap<>();

	public StackdriverLogbackMetrics() {
		this((LoggerContext) LoggerFactory.getILoggerFactory());
	}

	public StackdriverLogbackMetrics(LoggerContext context) {
		this.loggerContext = context;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		StackdriverMetricsTurboFilter filter = new StackdriverMetricsTurboFilter(registry, emptyList());
		metricsTurboFilters.put(registry, filter);
		loggerContext.addTurboFilter(filter);
	}

	@Override
	public void close() {
		for (StackdriverMetricsTurboFilter metricsTurboFilter : metricsTurboFilters.values()) {
			loggerContext.getTurboFilterList().remove(metricsTurboFilter);
		}
	}
}

@NonNullApi
@NonNullFields
class StackdriverMetricsTurboFilter extends TurboFilter {

	private final Counter alertCounter;
	private final Counter criticalCounter;
	private final Counter errorCounter;
	private final Counter warnCounter;
	private final Counter infoCounter;
	private final Counter debugCounter;
	private final Counter defaultCounter;

	StackdriverMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
		// emergency level is not in use

		alertCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("severity", "alert")
				.description("Number of alert severity events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		criticalCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("severity", "critical")
				.description("Number of critical severity events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		errorCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("severity", "error")
				.description("Number of error severity events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		warnCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("severity", "warning")
				.description("Number of warn severity events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		infoCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("severity", "info")
				.description("Number of info severity events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		debugCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("severity", "debug")
				.description("Number of debug severity events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		defaultCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("severity", "default")
				.description("Number of default severity events that made it to the logs")
				.baseUnit("events")
				.register(registry);
	}

	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
		Boolean ignored = StackdriverLogbackMetrics.ignoreMetrics.get();
		if (ignored != null && ignored) {
			return FilterReply.NEUTRAL;
		}

		// cannot use logger.isEnabledFor(level), as it would cause a StackOverflowError by calling this filter again!
		if (level.isGreaterOrEqual(logger.getEffectiveLevel()) && format != null) {
			switch (level.toInt()) {
			case Level.ERROR_INT:

				if(marker != null) {
					DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(marker);
					if(severity != null) {
						increment(severity);
					} else {
						errorCounter.increment();
					}
				} else {
					errorCounter.increment();
				}

				break;
			case Level.WARN_INT:
				warnCounter.increment();
				break;
			case Level.INFO_INT:
				infoCounter.increment();
				break;
			case Level.DEBUG_INT:
				debugCounter.increment();
				break;
			case Level.TRACE_INT:
				defaultCounter.increment();
				break;
			default : {
				// do nothing
			}
			}
		}

		return FilterReply.NEUTRAL;
	}

	private void increment(DevOpsLevel severity) {
		switch(severity) {
			case ERROR_WAKE_ME_UP_RIGHT_NOW: {
				alertCounter.increment();
				break;
			}
			case ERROR_INTERRUPT_MY_DINNER: {
				criticalCounter.increment();
				break;
			}
			case WARN: {
				warnCounter.increment();
				break;
			}
			case INFO: {
				infoCounter.increment();
				break;
			}
			case DEBUG: {
				debugCounter.increment();
				break;
			}
			case TRACE: {
				defaultCounter.increment();
				break;
			}
			case ERROR_TELL_ME_TOMORROW:
			default : {
				errorCounter.increment();
				break;
			}
		}
	}
}