package no.entur.logging.cloud.micrometer.counter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * {@link EventCounterFactory} that registers a Micrometer {@link Counter} (cumulative counter).
 *
 * <p>Used on Spring Boot 4.0.x, which ships Micrometer &lt; 1.17. In those versions,
 * {@code MetricsTurboFilter} registers {@code logback.events} as a {@code Counter}, so all
 * meters on the same metric name must use the same type to avoid an
 * {@code IllegalArgumentException} at registration time.
 *
 * @see FunctionEventCounterFactory
 */
public final class LegacyEventCounterFactory implements EventCounterFactory {

    @Override
    public EventCounter register(String metricName, MeterRegistry registry,
                                      Iterable<Tag> tags, String tagKey, String tagValue,
                                      String description) {
        Counter counter = Counter.builder(metricName)
                .tags(tags).tags(tagKey, tagValue)
                .description(description)
                .baseUnit("events")
                .register(registry);
        return new LegacyEventCounter(counter);
    }
}
