package no.entur.logging.cloud.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.boot.SpringBootVersion;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongConsumer;

/**
 * Compatibility wrapper that registers either a FunctionCounter (Spring Boot 4.1+) or a
 * Counter (Spring Boot 4.0.x), selected at class-load time via {@link SpringBootVersion}.
 */
public final class CompatibleCounter {

    /**
     * True for Spring Boot 4.1+, which ships Micrometer 1.17+ where
     * {@code MetricsTurboFilter} uses {@code FunctionCounter}.
     */
    static final boolean USE_FUNCTION_COUNTER = isSpringBoot41OrNewer();

    private static boolean isSpringBoot41OrNewer() {
        String version = SpringBootVersion.getVersion();
        if (version == null) {
            return true;
        }
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        return major > 4 || (major == 4 && minor >= 1);
    }

    private final Runnable inc;
    private final LongConsumer addN;

    private CompatibleCounter(Runnable inc, LongConsumer addN) {
        this.inc = inc;
        this.addN = addN;
    }

    public void increment() {
        inc.run();
    }

    public void add(long n) {
        addN.accept(n);
    }

    public static CompatibleCounter register(
            String metricName,
            MeterRegistry registry,
            Iterable<Tag> tags,
            String tagKey, String tagValue,
            String description) {
        if (USE_FUNCTION_COUNTER) {
            LongAdder adder = new LongAdder();
            FunctionCounter.builder(metricName, adder, LongAdder::doubleValue)
                    .tags(tags).tags(tagKey, tagValue)
                    .description(description)
                    .baseUnit("events")
                    .register(registry);
            return new CompatibleCounter(adder::increment, adder::add);
        } else {
            Counter counter = Counter.builder(metricName)
                    .tags(tags).tags(tagKey, tagValue)
                    .description(description)
                    .baseUnit("events")
                    .register(registry);
            return new CompatibleCounter(counter::increment, amount -> counter.increment((double) amount));
        }
    }
}
