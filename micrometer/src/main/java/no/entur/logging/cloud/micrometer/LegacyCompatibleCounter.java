package no.entur.logging.cloud.micrometer;

import io.micrometer.core.instrument.Counter;

final class LegacyCompatibleCounter implements CompatibleCounter {

    private final Counter counter;

    LegacyCompatibleCounter(Counter counter) {
        this.counter = counter;
    }

    @Override
    public void accept(long n) {
        counter.increment((double) n);
    }
}
