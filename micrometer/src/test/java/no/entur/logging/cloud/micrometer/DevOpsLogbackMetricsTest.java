package no.entur.logging.cloud.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
