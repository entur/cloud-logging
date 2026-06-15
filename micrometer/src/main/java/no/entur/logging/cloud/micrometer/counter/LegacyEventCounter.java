package no.entur.logging.cloud.micrometer.counter;

import io.micrometer.core.instrument.Counter;

/**
 * {@link EventCounter} implementation backed by a Micrometer {@link Counter} (cumulative counter).
 *
 * <p>Used on Spring Boot 4.0.x, which ships Micrometer &lt; 1.17. In those versions,
 * {@code MetricsTurboFilter} registers {@code logback.events} as a {@code Counter}, so all
 * meters on the same metric name must use the same type to avoid an
 * {@code IllegalArgumentException} at registration time.
 *
 * @see FunctionEventCounter
 * @see LegacyEventCounterFactory
 */
final class LegacyEventCounter implements EventCounter {

    private final Counter counter;

    LegacyEventCounter(Counter counter) {
        this.counter = counter;
    }

    @Override
    public void accept(long n) {
        counter.increment((double) n);
    }
}
