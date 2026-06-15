package no.entur.logging.cloud.spring.stderr.micrometer;

import no.entur.logging.cloud.micrometer.CompatibleCounter;
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

    private final PrintStream originalSystemErr;
    private final CompatibleCounter errorCount;
    private final CompatibleCounter errorTellMeTomorrowCount;

    public SystemErrMicrometerPrintStream(PrintStream originalSystemErr,
                                          CompatibleCounter errorCount,
                                          CompatibleCounter errorTellMeTomorrowCount) {
        super(originalSystemErr, true);
        this.originalSystemErr = originalSystemErr;
        this.errorCount = errorCount;
        this.errorTellMeTomorrowCount = errorTellMeTomorrowCount;
    }

    @Override
    public void write(int b) {
        super.write(b);
        if (b == '\n') {
            errorCount.accept(1L);
            errorTellMeTomorrowCount.accept(1L);
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
            errorCount.accept(newlines);
            errorTellMeTomorrowCount.accept(newlines);
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
