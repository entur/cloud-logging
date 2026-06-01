package no.entur.logging.cloud.spring.stderr.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.DisposableBean;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A {@link PrintStream} that intercepts all output written to {@code System.err} and increments
 * Micrometer error counters on each newline character.
 *
 * <p>Uses the same {@code logback.events} metric name and {@code level} tags as
 * {@code DevOpsMetricsTurboFilter} so that stderr lines are counted alongside regular
 * log-level events in existing dashboards.
 *
 * <p>All bytes are forwarded to the original {@code System.err} PrintStream unchanged.
 *
 * <p>Restores the original {@code System.err} when {@link #destroy()} is called.
 */
public class SystemErrMicrometerPrintStream extends PrintStream implements DisposableBean {

    private final PrintStream originalSystemErr;
    private final Counter errorCounter;
    private final Counter errorTellMeTomorrowCounter;

    public SystemErrMicrometerPrintStream(MeterRegistry registry, PrintStream originalSystemErr) {
        // Null output stream avoids double-writing: write() overrides delegate directly to originalSystemErr.
        super(OutputStream.nullOutputStream());
        this.originalSystemErr = originalSystemErr;

        this.errorCounter = Counter.builder("logback.events")
                .tags("level", "error")
                .description("Number of all error level events that made it to the logs")
                .baseUnit("events")
                .register(registry);

        this.errorTellMeTomorrowCounter = Counter.builder("logback.events")
                .tags("level", "errorTellMeTomorrow")
                .description("Number of error 'Tell Me Tomorrow' level events that made it to the logs")
                .baseUnit("events")
                .register(registry);
    }

    @Override
    public void write(int b) {
        originalSystemErr.write(b);
        if (b == '\n') {
            errorCounter.increment();
            errorTellMeTomorrowCounter.increment();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        originalSystemErr.write(b, off, len);
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
    public void flush() {
        originalSystemErr.flush();
    }

    @Override
    public void close() {
        originalSystemErr.close();
    }

    @Override
    public void destroy() {
        if (System.err == this) {
            System.setErr(originalSystemErr);
        }
    }
}
