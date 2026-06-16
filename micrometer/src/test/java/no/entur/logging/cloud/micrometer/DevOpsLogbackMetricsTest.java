package no.entur.logging.cloud.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
	 * Simulates the Micrometer 1.17+ scenario where the built-in {@code MetricsTurboFilter}
	 * has already registered {@code FunctionCounter} meters for standard log levels before
	 * our {@code DevOpsMetricsTurboFilter} is initialised.
	 * <p>
	 * Without the {@code captureOrRegister} helper this would throw
	 * {@link IllegalArgumentException}: "Meter with the same name but different types …".
	 */
	@Test
	public void givenFunctionCountersAlreadyRegistered_whenCreatingFilter_thenNoException() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		// Pre-register FunctionCounters for the five standard levels — mimicking what
		// Micrometer 1.17's MetricsTurboFilter does before our filter initialises.
		for (String level : new String[]{"error", "warn", "info", "debug", "trace"}) {
			LongAdder adder = new LongAdder();
			FunctionCounter.builder("logback.events", adder, LongAdder::doubleValue)
					.tag("level", level)
					.register(registry);
		}

		// Must not throw even though FunctionCounters already occupy the standard-level slots.
		assertDoesNotThrow(() -> new DevOpsMetricsTurboFilter(registry, emptyList()));
	}
}
