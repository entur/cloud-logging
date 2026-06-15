package no.entur.logging.cloud.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

public final class LegacyCompatibleCounterFactory implements CompatibleCounterFactory {

    @Override
    public CompatibleCounter register(String metricName, MeterRegistry registry,
                                      Iterable<Tag> tags, String tagKey, String tagValue,
                                      String description) {
        Counter counter = Counter.builder(metricName)
                .tags(tags).tags(tagKey, tagValue)
                .description(description)
                .baseUnit("events")
                .register(registry);
        return new LegacyCompatibleCounter(counter);
    }
}
