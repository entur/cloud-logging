package no.entur.logging.cloud.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ch.qos.logback.classic.Level;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static java.util.Collections.emptyList;

public class DevOpsLogbackMetricsTest {

	private static DevOpsLogger log = DevOpsLoggerFactory.getLogger(DevOpsLogbackMetricsTest.class);
	
	@Test
	public void givenGlobalRegistry_whenLogging_thenCounted() {
		SimpleMeterRegistry oneSimpleMeter = new SimpleMeterRegistry();
	    Metrics.addRegistry(oneSimpleMeter);

		DevOpsLogbackMetrics m = new DevOpsLogbackMetrics();

	    m.bindTo(oneSimpleMeter);
	    
	    log.errorTellMeTomorrow("Error statement");
	    log.errorWakeMeUpRightNow("Alert statement");
	    log.errorWakeMeUpRightNow("Alert statement");
	    log.errorInterruptMyDinner("Critical statement");
	    log.errorInterruptMyDinner("Critical statement");
	    log.errorInterruptMyDinner("Critical statement");

	    Collection<Counter> counters = oneSimpleMeter.find("logback.events").counters();

	    Optional<Counter> error = counters.stream().filter(counter -> counter.getId().getTag("level").equals("errorTellMeTomorrow")).findAny();
	    assertTrue(error.isPresent());
	    assertThat(error.get().count()).isEqualTo(1.0);
	    
	    Optional<Counter> alert = counters.stream().filter(counter -> counter.getId().getTag("level").equals("errorWakeMeUpRightNow")).findAny();
	    assertTrue(alert.isPresent());
	    assertThat(alert.get().count()).isEqualTo(2.0);

	    Optional<Counter> critical = counters.stream().filter(counter -> counter.getId().getTag("level").equals("errorInterruptMyDinner")).findAny();
	    assertTrue(critical.isPresent());
	    assertThat(critical.get().count()).isEqualTo(3.0);
	}

	/**
	 * Simulates a scenario where non-{@code Counter} meters (e.g., {@code FunctionCounter})
	 * already occupy the standard-level slots.  {@code captureOrRegister} must remove them
	 * and register fresh {@link Counter} instances so that incrementing always works.
	 */
	@Test
	public void givenFunctionCountersAlreadyRegistered_whenCreatingFilter_thenCountersAreIncrementable() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		// Pre-register FunctionCounters for the five standard levels.
		for (String level : new String[]{"error", "warn", "info", "debug", "trace"}) {
			LongAdder adder = new LongAdder();
			FunctionCounter.builder("logback.events", adder, LongAdder::doubleValue)
					.tag("level", level)
					.register(registry);
		}

		// captureOrRegister must replace each FunctionCounter with an incrementable Counter.
		DevOpsMetricsTurboFilter filter = new DevOpsMetricsTurboFilter(registry, emptyList());

		// Exercise the increment(Marker, Level) path which drives the standard-level counters.
		filter.increment(null, Level.ERROR);
		filter.increment(null, Level.WARN);
		filter.increment(null, Level.INFO);
		filter.increment(null, Level.DEBUG);
		filter.increment(null, Level.TRACE);

		assertThat(registry.find("logback.events").tag("level", "error").counter().count()).isEqualTo(1.0);
		assertThat(registry.find("logback.events").tag("level", "warn").counter().count()).isEqualTo(1.0);
		assertThat(registry.find("logback.events").tag("level", "info").counter().count()).isEqualTo(1.0);
		assertThat(registry.find("logback.events").tag("level", "debug").counter().count()).isEqualTo(1.0);
		assertThat(registry.find("logback.events").tag("level", "trace").counter().count()).isEqualTo(1.0);
	}
}
