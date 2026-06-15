package no.entur.logging.cloud.spring.stderr.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.SpringBootVersion;

import java.io.PrintStream;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongConsumer;

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

    private static final boolean USE_FUNCTION_COUNTER = isSpringBoot41OrNewer();

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

    private final PrintStream originalSystemErr;
    private final Runnable incError;
    private final LongConsumer addError;
    private final Runnable incErrorTellMeTomorrow;
    private final LongConsumer addErrorTellMeTomorrow;

    public SystemErrMicrometerPrintStream(MeterRegistry registry, PrintStream originalSystemErr) {
        super(originalSystemErr, true);
        this.originalSystemErr = originalSystemErr;

        if (USE_FUNCTION_COUNTER) {
            LongAdder errorAdder = new LongAdder();
            FunctionCounter.builder("logback.events", errorAdder, LongAdder::doubleValue)
                    .tags("level", "error")
                    .description("Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)")
                    .baseUnit("events")
                    .register(registry);
            incError = errorAdder::increment;
            addError = errorAdder::add;

            LongAdder errorTellMeTomorrowAdder = new LongAdder();
            FunctionCounter.builder("logback.events", errorTellMeTomorrowAdder, LongAdder::doubleValue)
                    .tags("level", "errorTellMeTomorrow")
                    .description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
                    .baseUnit("events")
                    .register(registry);
            incErrorTellMeTomorrow = errorTellMeTomorrowAdder::increment;
            addErrorTellMeTomorrow = errorTellMeTomorrowAdder::add;
        } else {
            Counter errorCounter = Counter.builder("logback.events")
                    .tags("level", "error")
                    .description("Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)")
                    .baseUnit("events")
                    .register(registry);
            incError = errorCounter::increment;
            addError = amount -> errorCounter.increment((double) amount);

            Counter errorTellMeTomorrowCounter = Counter.builder("logback.events")
                    .tags("level", "errorTellMeTomorrow")
                    .description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
                    .baseUnit("events")
                    .register(registry);
            incErrorTellMeTomorrow = errorTellMeTomorrowCounter::increment;
            addErrorTellMeTomorrow = amount -> errorTellMeTomorrowCounter.increment((double) amount);
        }
    }

    @Override
    public void write(int b) {
        super.write(b);
        if (b == '\n') {
            incError.run();
            incErrorTellMeTomorrow.run();
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
            addError.accept(newlines);
            addErrorTellMeTomorrow.accept(newlines);
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

