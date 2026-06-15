package no.entur.logging.cloud.micrometer.counter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * Factory that creates and registers an {@link EventCounter}.
 *
 * <p>Use {@link #forCurrentMicrometerVersion()} to obtain the appropriate factory
 * for the runtime Micrometer version. Micrometer 1.17+ registers {@code logback.events}
 * as a {@code FunctionCounter}; earlier versions use a cumulative {@code Counter}.
 */
public interface EventCounterFactory {

    EventCounter register(String metricName, MeterRegistry registry,
                               Iterable<Tag> tags, String tagKey, String tagValue,
                               String description);

    static EventCounterFactory forCurrentMicrometerVersion() {
        Package pkg = Counter.class.getPackage();
        String version = pkg != null ? pkg.getImplementationVersion() : null;
        if (version != null) {
            try {
                String[] parts = version.split("\\.");
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                if (major < 1 || (major == 1 && minor < 17)) {
                    return new LegacyEventCounterFactory();
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return new FunctionEventCounterFactory();
    }
}
