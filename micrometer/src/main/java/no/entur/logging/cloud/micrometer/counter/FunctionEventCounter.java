package no.entur.logging.cloud.micrometer.counter;

import java.util.concurrent.atomic.LongAdder;

final class FunctionEventCounter implements EventCounter {

    private final LongAdder adder;

    FunctionEventCounter(LongAdder adder) {
        this.adder = adder;
    }

    @Override
    public void accept(long n) {
        adder.add(n);
    }
}
