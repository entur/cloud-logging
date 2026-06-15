package no.entur.logging.cloud.micrometer.counter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.boot.SpringBootVersion;

/**
 * Factory that creates and registers a {@link EventCounter}.
 *
 * <p>Use {@link #forCurrentSpringBootVersion()} to obtain the appropriate factory
 * for the runtime Spring Boot version.
 */
public interface EventCounterFactory {

    EventCounter register(String metricName, MeterRegistry registry,
                               Iterable<Tag> tags, String tagKey, String tagValue,
                               String description);

    static EventCounterFactory forCurrentSpringBootVersion() {
        String version = SpringBootVersion.getVersion();
        if (version != null) {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            if (major < 4 || (major == 4 && minor < 1)) {
                return new LegacyEventCounterFactory();
            }
        }
        return new FunctionEventCounterFactory();
    }
}
