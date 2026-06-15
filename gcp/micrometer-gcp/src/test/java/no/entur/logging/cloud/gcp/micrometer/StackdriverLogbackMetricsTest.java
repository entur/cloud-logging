package no.entur.logging.cloud.gcp.micrometer;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.google.common.truth.Truth.assertThat;

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

	    assertThat(getCount(oneSimpleMeter, "logback.gcp.events", "severity", "error")).isEqualTo(1.0);
	    assertThat(getCount(oneSimpleMeter, "logback.gcp.events", "severity", "alert")).isEqualTo(2.0);
	    assertThat(getCount(oneSimpleMeter, "logback.gcp.events", "severity", "critical")).isEqualTo(3.0);
	}

	private double getCount(SimpleMeterRegistry registry, String metricName, String tagKey, String tagValue) {
		Optional<Meter> meter = registry.find(metricName).tag(tagKey, tagValue).meters().stream().findAny();
		if (meter.isEmpty()) return 0.0;
		return StreamSupport.stream(meter.get().measure().spliterator(), false)
				.filter(ms -> ms.getStatistic() == Statistic.COUNT)
				.mapToDouble(Measurement::getValue)
				.sum();
	}
}
