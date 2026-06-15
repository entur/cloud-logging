package no.entur.logging.cloud.micrometer;

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

	    assertThat(getCount(oneSimpleMeter, "logback.events", "level", "errorTellMeTomorrow")).isEqualTo(1.0);
	    assertThat(getCount(oneSimpleMeter, "logback.events", "level", "errorWakeMeUpRightNow")).isEqualTo(2.0);
	    assertThat(getCount(oneSimpleMeter, "logback.events", "level", "errorInterruptMyDinner")).isEqualTo(3.0);
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
