package no.entur.logging.cloud.micrometer.counter;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.concurrent.atomic.LongAdder;

public final class FunctionEventCounterFactory implements EventCounterFactory {

    @Override
    public EventCounter register(String metricName, MeterRegistry registry,
                                      Iterable<Tag> tags, String tagKey, String tagValue,
                                      String description) {
        LongAdder adder = new LongAdder();
        FunctionCounter.builder(metricName, adder, LongAdder::doubleValue)
                .tags(tags).tags(tagKey, tagValue)
                .description(description)
                .baseUnit("events")
                .register(registry);
        return new FunctionEventCounter(adder);
    }
}
