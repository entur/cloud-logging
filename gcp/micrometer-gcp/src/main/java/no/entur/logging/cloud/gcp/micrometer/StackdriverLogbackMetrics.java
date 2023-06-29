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
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverLogSeverityJsonProvider;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverSeverity;
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

	private final Counter emergencyCounter;
	private final Counter alertCounter;
	private final Counter criticalCounter;
	private final Counter errorCounter;
	private final Counter warnCounter;
	private final Counter infoCounter;
	private final Counter debugCounter;
	private final Counter traceCounter;

	StackdriverMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
		emergencyCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("level", "emergency")
				.description("Number of emergency level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		alertCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("level", "alert")
				.description("Number of alert level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		criticalCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("level", "critical")
				.description("Number of critical level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		errorCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("level", "error")
				.description("Number of error level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		warnCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("level", "warn")
				.description("Number of warn level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		infoCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("level", "info")
				.description("Number of info level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		debugCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("level", "debug")
				.description("Number of debug level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		traceCounter = Counter.builder("logback.gcp.events")
				.tags(tags).tags("level", "trace")
				.description("Number of trace level events that made it to the logs")
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
					StackdriverSeverity severity = StackdriverLogSeverityJsonProvider.searchSeverityMarker(marker);
					if(severity != null) {
						switch(severity) {
							case EMERGENCY : {
								emergencyCounter.increment();
								break;
							}
							case ALERT : {
								alertCounter.increment();
								break;
							}
							case CRITICAL : {
								criticalCounter.increment();
								break;
							}
		
							default : {
								errorCounter.increment();
							}
						}
						break;
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
				traceCounter.increment();
				break;
			default : {
				// do nothing
			}
			}
		}

		return FilterReply.NEUTRAL;
	}
}