package no.entur.logging.cloud.micrometer;

import java.util.concurrent.atomic.LongAdder;

final class FunctionCompatibleCounter implements CompatibleCounter {

    private final LongAdder adder;

    FunctionCompatibleCounter(LongAdder adder) {
        this.adder = adder;
    }

    @Override
    public void accept(long n) {
        adder.add(n);
    }
}
