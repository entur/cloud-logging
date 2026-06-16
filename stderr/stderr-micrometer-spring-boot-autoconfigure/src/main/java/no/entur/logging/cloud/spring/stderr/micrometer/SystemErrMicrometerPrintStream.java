package no.entur.logging.cloud.spring.stderr.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.DisposableBean;

import java.io.PrintStream;

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
public class SystemErrMicrometerPrintStream extends PrintStream implements DisposableBean {

    /**
     * No-op counter used when a non-{@code Counter} meter (e.g., a {@code FunctionCounter}
     * registered by Micrometer 1.17+'s built-in {@code MetricsTurboFilter}) already occupies
     * the same metric ID.  Incrementing it is safe but has no observable effect.
     */
    private static final Counter NOOP_COUNTER = Counter.builder("noop").register(new SimpleMeterRegistry());

    private final PrintStream originalSystemErr;
    private final Counter errorCounter;
    private final Counter errorTellMeTomorrowCounter;

    public SystemErrMicrometerPrintStream(MeterRegistry registry, PrintStream originalSystemErr) {
        super(originalSystemErr, true);
        this.originalSystemErr = originalSystemErr;

        // logback.events[level=error] is also registered by Micrometer's built-in
        // MetricsTurboFilter (FunctionCounter in Micrometer 1.17+).  Use
        // captureOrRegister to avoid an IllegalArgumentException on type mismatch.
        this.errorCounter = captureOrRegister(registry, "logback.events", "level", "error",
                "Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)");

        // logback.events[level=errorTellMeTomorrow] is DevOps-specific and never
        // registered by Micrometer's built-in filter, so plain registration is safe.
        this.errorTellMeTomorrowCounter = Counter.builder("logback.events")
                .tags("level", "errorTellMeTomorrow")
                .description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);
    }

    /**
     * Captures an existing {@link Counter} from the registry, or registers a fresh one.
     * Returns {@link #NOOP_COUNTER} when a non-{@code Counter} meter already owns the ID.
     */
    private static Counter captureOrRegister(MeterRegistry registry, String name,
            String tagKey, String tagValue, String description) {
        Meter existing = registry.find(name).tag(tagKey, tagValue).meter();
        if (existing instanceof Counter c) {
            return c;
        } else if (existing != null) {
            return NOOP_COUNTER;
        }
        return Counter.builder(name)
                .tags(tagKey, tagValue)
                .description(description)
                .baseUnit("events")
                .register(registry);
    }

    @Override
    public void write(int b) {
        super.write(b);
        if (b == '\n') {
            errorCounter.increment();
            errorTellMeTomorrowCounter.increment();
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
            errorCounter.increment(newlines);
            errorTellMeTomorrowCounter.increment(newlines);
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
