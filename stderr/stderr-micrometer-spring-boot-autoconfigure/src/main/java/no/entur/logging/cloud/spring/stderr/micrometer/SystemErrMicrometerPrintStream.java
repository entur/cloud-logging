package no.entur.logging.cloud.spring.stderr.micrometer;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.io.PrintStream;
import java.util.concurrent.atomic.LongAdder;

/**
 * A {@link PrintStream} that intercepts all output written to {@code System.err} and increments
 * Micrometer error counters on each newline character.
 *
 * <p>Uses the same {@code logback.events} metric name and {@code level} tags as
 * {@code DevOpsMetricsTurboFilter} so that stderr lines are counted alongside regular
 * log-level events in existing dashboards.
 *
 * <p>All bytes are forwarded to the original {@code System.err} PrintStream unchanged via the
 * superclass. The following methods are overridden to intercept all possible write paths:
 * {@link #write(int)}, {@link #write(byte[], int, int)}, and {@link #write(byte[])}.
 * All higher-level {@link java.io.PrintStream} paths ({@code print}, {@code println},
 * {@code format}, {@code printf}, {@code writeBytes}, etc.) ultimately route through one of
 * these three methods, either via the internal {@code charOut = new OutputStreamWriter(this)}
 * chain (for character-based methods) or via direct delegation (for {@code writeBytes}).
 *
 * <p>Restores the original {@code System.err} when {@link #destroy()} is called.
 */
public class SystemErrMicrometerPrintStream extends PrintStream implements DisposableBean, SmartInitializingSingleton {

    private final PrintStream originalSystemErr;
    private final MeterRegistry registry;
    private final LongAdder errorAdder = new LongAdder();
    private final LongAdder errorTellMeTomorrowAdder = new LongAdder();

    public SystemErrMicrometerPrintStream(MeterRegistry registry, PrintStream originalSystemErr) {
        super(originalSystemErr, true);
        this.originalSystemErr = originalSystemErr;
        this.registry = registry;
        // Meter registration is deferred to afterSingletonsInstantiated() so that
        // MeterRegistryPostProcessor (a BeanPostProcessor created early) has already applied
        // all MeterBinders — including the built-in LogbackMetrics — before we register.
        // This avoids a type-mismatch conflict when the built-in LogbackMetrics registers
        // a plain Counter for logback.events[level=error] (pre-1.17 Micrometer).
    }

    /**
     * Registers the FunctionCounter meters after all singletons have been instantiated.
     *
     * <p>At this point {@code MeterRegistryPostProcessor} has already applied its
     * {@code MeterBinder}s (e.g., the built-in {@code LogbackMetrics}), so any
     * pre-existing plain Counter can be safely replaced by a FunctionCounter.
     */
    @Override
    public void afterSingletonsInstantiated() {
        // logback.events[level=error] may have been registered as a plain Counter by
        // Micrometer's built-in LogbackMetrics (pre-1.17). removeLegacyCounter() removes
        // it so our FunctionCounter can be registered without a type-mismatch error.
        // TODO: Legacy - removeLegacyCounter() call below can be deleted once pre-1.17
        //       Micrometer is no longer supported.
        removeLegacyCounter(registry, "logback.events", "level", "error");
        FunctionCounter.builder("logback.events", errorAdder, LongAdder::doubleValue)
                .tags("level", "error")
                .description("Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)")
                .baseUnit("events")
                .register(registry);

        // logback.events[level=errorTellMeTomorrow] is DevOps-specific and never
        // registered by Micrometer's built-in filter, so plain registration is safe.
        FunctionCounter.builder("logback.events", errorTellMeTomorrowAdder, LongAdder::doubleValue)
                .tags("level", "errorTellMeTomorrow")
                .description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);
    }

    /**
     * Removes a plain Counter meter (if present) from the registry so that a FunctionCounter
     * can be registered for the same ID without a type-mismatch error.
     * <p>
     * TODO: Legacy - this method can be deleted once pre-1.17 Micrometer is no longer supported.
     */
    private static void removeLegacyCounter(MeterRegistry registry, String name,
            String tagKey, String tagValue) {
        Meter existing = registry.find(name).tag(tagKey, tagValue).meter();
        if (existing != null && !(existing instanceof FunctionCounter)) {
            registry.remove(existing);
        }
    }

    @Override
    public void write(int b) {
        super.write(b);
        if (b == '\n') {
            errorAdder.increment();
            errorTellMeTomorrowAdder.increment();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        super.write(b, off, len);
        int newlines = 0;
        for (int i = off; i < off + len; i++) {
            if (b[i] == '\n') {
                newlines++;
            }
        }
        if (newlines > 0) {
            errorAdder.add(newlines);
            errorTellMeTomorrowAdder.add(newlines);
        }
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void destroy() {
        if (System.err == this) {
            System.setErr(originalSystemErr);
        }
    }
}
