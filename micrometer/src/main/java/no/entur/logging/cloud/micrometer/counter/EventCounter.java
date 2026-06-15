package no.entur.logging.cloud.micrometer.counter;

import java.util.function.LongConsumer;

/**
 * Abstraction over a Micrometer counter that works with both
 * {@code FunctionCounter} (Spring Boot 4.1+ / Micrometer 1.17+) and
 * {@code Counter} (Spring Boot 4.0.x).
 *
 * <p>Call {@link #accept(long)} to increment the counter by {@code n}.
 */
public interface EventCounter extends LongConsumer {
}
