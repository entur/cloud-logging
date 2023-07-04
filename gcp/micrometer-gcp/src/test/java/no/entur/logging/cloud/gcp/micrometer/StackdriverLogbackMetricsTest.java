package no.entur.logging.cloud.gcp.micrometer;

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

public class StackdriverLogbackMetricsTest {

	private static DevOpsLogger log = DevOpsLoggerFactory.getLogger(StackdriverLogbackMetricsTest.class);
	
	@Test
	public void givenGlobalRegistry_whenLogging_thenCounted() {
		SimpleMeterRegistry oneSimpleMeter = new SimpleMeterRegistry();
	    Metrics.addRegistry(oneSimpleMeter);

	    StackdriverLogbackMetrics m = new StackdriverLogbackMetrics();

	    m.bindTo(oneSimpleMeter);
	    
	    log.errorTellMeTomorrow("Error statement");
	    log.errorWakeMeUpRightNow("Alert statement");
	    log.errorWakeMeUpRightNow("Alert statement");
	    log.errorInterruptMyDinner("Critical statement");
	    log.errorInterruptMyDinner("Critical statement");
	    log.errorInterruptMyDinner("Critical statement");

	    Collection<Counter> counters = oneSimpleMeter.find("logback.gcp.events").counters();

	    Optional<Counter> error = counters.stream().filter(counter -> counter.getId().getTag("severity").equals("error")).findAny();
	    assertTrue(error.isPresent());
	    assertThat(error.get().count()).isEqualTo(1.0);
	    
	    Optional<Counter> alert = counters.stream().filter(counter -> counter.getId().getTag("severity").equals("alert")).findAny();
	    assertTrue(alert.isPresent());
	    assertThat(alert.get().count()).isEqualTo(2.0);

	    Optional<Counter> critical = counters.stream().filter(counter -> counter.getId().getTag("severity").equals("critical")).findAny();
	    assertTrue(critical.isPresent());
	    assertThat(critical.get().count()).isEqualTo(3.0);
	}
}
