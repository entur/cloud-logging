package no.entur.logging.cloud.micrometer;

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
public class DevOpsLogbackMetrics extends io.micrometer.core.instrument.binder.logging.LogbackMetrics { // extend since there is no interface type
	static ThreadLocal<Boolean> ignoreMetrics = new ThreadLocal<>();

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
		DevOpsMetricsTurboFilter filter = new DevOpsMetricsTurboFilter(registry, emptyList());
		metricsTurboFilters.put(registry, filter);
		loggerContext.addTurboFilter(filter);
	}

	@Override
	public void close() {
		for (DevOpsMetricsTurboFilter metricsTurboFilter : metricsTurboFilters.values()) {
			loggerContext.getTurboFilterList().remove(metricsTurboFilter);
		}
	}
}

@NonNullApi
@NonNullFields
class DevOpsMetricsTurboFilter extends TurboFilter {

	private final Counter errorWakeMeUpRightNowCounter;
	private final Counter errorInterruptMyDinnerCounter;
	private final Counter errorTellMeTomorrowCounter;

	private final Counter errorCounter; // alias for all errors / backwards compatibility

	private final Counter warnCounter;
	private final Counter infoCounter;
	private final Counter debugCounter;
	private final Counter traceCounter;

	DevOpsMetricsTurboFilter(MeterRegistry registry, Iterable<Tag> tags) {
		errorWakeMeUpRightNowCounter = Counter.builder("logback.events")
				.tags(tags).tags("level", "errorWakeMeUpRightNow")
				.description("Number of error 'Wake Me Up Right Now' level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		errorInterruptMyDinnerCounter = Counter.builder("logback.events")
				.tags(tags).tags("level", "errorInterruptMyDinner")
				.description("Number of error 'Interrupt My Dinner' level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		errorTellMeTomorrowCounter = Counter.builder("logback.events")
				.tags(tags).tags("level", "errorTellMeTomorrow")
				.description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		errorCounter = Counter.builder("logback.events")
				.tags(tags).tags("level", "error")
				.description("Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)")
				.baseUnit("events")
				.register(registry);

		warnCounter = Counter.builder("logback.events")
				.tags(tags).tags("level", "warn")
				.description("Number of warn level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		infoCounter = Counter.builder("logback.events")
				.tags(tags).tags("level", "info")
				.description("Number of info level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		debugCounter = Counter.builder("logback.events")
				.tags(tags).tags("level", "debug")
				.description("Number of debug level events that made it to the logs")
				.baseUnit("events")
				.register(registry);

		traceCounter = Counter.builder("logback.events")
				.tags(tags).tags("level", "trace")
				.description("Number of trace level events that made it to the logs")
				.baseUnit("events")
				.register(registry);
	}

	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
		Boolean ignored = DevOpsLogbackMetrics.ignoreMetrics.get();
		if (ignored != null && ignored) {
			return FilterReply.NEUTRAL;
		}

		// cannot use logger.isEnabledFor(level), as it would cause a StackOverflowError by calling this filter again!
		if (level.isGreaterOrEqual(logger.getEffectiveLevel()) && format != null) {
			switch (level.toInt()) {
			case Level.ERROR_INT:
				errorCounter.increment();

				if(marker != null) {
					DevOpsLevel severity = DevOpsMarker.searchSeverityMarker(marker);
					if(severity != null) {
						increment(severity);
					} else  {
						errorTellMeTomorrowCounter.increment();
					}
				} else {
					errorTellMeTomorrowCounter.increment();
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

	private void increment(DevOpsLevel severity) {
		switch(severity) {
			case ERROR_WAKE_ME_UP_RIGHT_NOW : {
				errorWakeMeUpRightNowCounter.increment();
				break;
			}
			case ERROR_INTERRUPT_MY_DINNER : {
				errorInterruptMyDinnerCounter.increment();
				break;
			}
			case ERROR_TELL_ME_TOMORROW: {
				errorTellMeTomorrowCounter.increment();
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
				traceCounter.increment();
				break;
			}
			default : {
				errorTellMeTomorrowCounter.increment();
			}
		}
	}
}