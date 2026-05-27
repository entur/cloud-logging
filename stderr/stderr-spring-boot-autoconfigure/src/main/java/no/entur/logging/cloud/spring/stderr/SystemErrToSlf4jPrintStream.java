package no.entur.logging.cloud.spring.stderr;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.DisposableBean;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.StackWalker.StackFrame;

/**
 * A {@link PrintStream} that intercepts all output written to {@code System.err} and forwards it
 * to an SLF4J {@link Logger}.
 *
 * <h3>Detection strategy</h3>
 * <p>{@link Throwable#printStackTrace(PrintStream)} exclusively uses {@link #println(Object)} –
 * one call per line – routing the output through its internal {@code WrappedPrintStream}.
 * This class overrides {@link #println(Object)} and uses {@link StackWalker} to check whether
 * {@code Throwable.printStackTrace} is present in the current thread's call stack.  When it is,
 * lines are accumulated in a per-thread buffer; the complete trace is emitted as a single SLF4J
 * log statement once the next non-{@code printStackTrace} write arrives or when {@link #flush()}
 * is called.  Every other {@link #println} call is forwarded immediately without buffering.
 *
 * <h3>Thread safety</h3>
 * <p>Stack-trace accumulation uses a {@link ThreadLocal} buffer so concurrent
 * {@code printStackTrace} calls from different threads do not interfere with each other.
 *
 * <p>Instances of this class save the previous {@code System.err} reference and restore it when
 * the Spring application context is closed (via the {@link DisposableBean} contract).
 */
public class SystemErrToSlf4jPrintStream extends PrintStream implements DisposableBean {

    /**
     * Guard against infinite recursion that would occur if the SLF4J backend itself wrote to
     * {@code System.err} (e.g. during its own initialisation or error reporting).
     */
    private static final ThreadLocal<Boolean> LOGGING_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Per-thread accumulator for lines produced by {@link Throwable#printStackTrace(PrintStream)}.
     * Using a {@link ThreadLocal} allows multiple threads to call {@code printStackTrace}
     * concurrently without interfering with each other's buffers.
     */
    private static final ThreadLocal<StringBuilder> STACK_TRACE_BUFFER = ThreadLocal.withInitial(StringBuilder::new);

    /**
     * Lazy {@link StackWalker} used to determine whether the current {@link #println(Object)}
     * call originates from {@link Throwable#printStackTrace}.  The walker stops as soon as a
     * matching frame is found, making the check inexpensive for the common case.
     */
    private static final StackWalker STACK_WALKER = StackWalker.getInstance();

    private final Logger logger;
    private final Level level;
    private final PrintStream originalSystemErr;

    // Buffer for the raw write() path (print(String), println(int), etc.) – guarded by 'this'
    private final StringBuilder rawLineBuffer = new StringBuilder();

    /**
     * @param logger            the SLF4J logger to write forwarded output to
     * @param level             the log level to use
     * @param originalSystemErr the original {@code System.err} stream to restore on {@link #destroy()}
     */
    public SystemErrToSlf4jPrintStream(Logger logger, Level level, PrintStream originalSystemErr) {
        super(OutputStream.nullOutputStream());
        this.logger = logger;
        this.level = level;
        this.originalSystemErr = originalSystemErr;
    }

    // -------------------------------------------------------------------------
    // println interception
    // -------------------------------------------------------------------------

    /**
     * Primary interception point for {@link Throwable#printStackTrace(PrintStream)}, which
     * exclusively calls {@code println(Object)} (never {@code println(String)}) once per line.
     *
     * <p>When a {@code Throwable.printStackTrace} call is detected in the call stack the line is
     * appended to a per-thread buffer.  Otherwise any pending stack-trace buffer for this thread
     * is flushed first and the new line is emitted immediately.
     */
    @Override
    public void println(Object x) {
        String line = String.valueOf(x);
        if (isCalledFromPrintStackTrace()) {
            appendToStackTraceBuffer(line);
        } else {
            flushStackTraceBuffer();
            emit(line);
        }
    }

    /**
     * {@link Throwable#printStackTrace(PrintStream)} never calls {@code println(String)} directly;
     * this method therefore flushes any pending per-thread stack-trace buffer and emits the line
     * immediately without performing a stack walk.
     */
    @Override
    public void println(String x) {
        flushStackTraceBuffer();
        emit(x != null ? x : "null");
    }

    // -------------------------------------------------------------------------
    // Raw write fallback – handles print(String), println(int/long/…), etc.
    // These methods are never called by Throwable.printStackTrace.
    // -------------------------------------------------------------------------

    @Override
    public synchronized void write(int b) {
        if (b == '\n') {
            String line = rawLineBuffer.toString();
            rawLineBuffer.setLength(0);
            emit(line);
        } else if (b != '\r') {
            rawLineBuffer.append((char) b);
        }
    }

    @Override
    public synchronized void write(byte[] buf, int off, int len) {
        for (int i = off; i < off + len; i++) {
            write(buf[i] & 0xff);
        }
    }

    // -------------------------------------------------------------------------
    // Stack-trace detection helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when {@code java.lang.Throwable.printStackTrace} appears anywhere in
     * the current thread's call stack.  The {@link StackWalker} stream is lazy and short-circuits
     * as soon as a matching frame is found.
     */
    private boolean isCalledFromPrintStackTrace() {
        return STACK_WALKER.walk(frames ->
            frames.anyMatch(f ->
                "java.lang.Throwable".equals(f.getClassName()) &&
                "printStackTrace".equals(f.getMethodName())
            )
        );
    }

    private void appendToStackTraceBuffer(String line) {
        StringBuilder buf = STACK_TRACE_BUFFER.get();
        if (buf.length() > 0) {
            buf.append('\n');
        }
        buf.append(line);
    }

    /**
     * Emits the accumulated per-thread stack-trace buffer as a single log statement and resets
     * the buffer.  A no-op when the buffer is empty.
     */
    private void flushStackTraceBuffer() {
        StringBuilder buf = STACK_TRACE_BUFFER.get();
        if (buf.length() > 0) {
            String message = buf.toString();
            buf.setLength(0);
            emit(message);
        }
    }

    // -------------------------------------------------------------------------
    // Emit
    // -------------------------------------------------------------------------

    /** Forwards a (possibly multi-line) message to the SLF4J logger at the configured level. */
    private void emit(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (LOGGING_GUARD.get()) {
            return; // prevent infinite recursion
        }
        LOGGING_GUARD.set(Boolean.TRUE);
        try {
            switch (level) {
                case ERROR: logger.error(message); break;
                case WARN:  logger.warn(message);  break;
                case INFO:  logger.info(message);  break;
                case DEBUG: logger.debug(message); break;
                case TRACE: logger.trace(message); break;
                default:    logger.error(message); break;
            }
        } finally {
            LOGGING_GUARD.set(Boolean.FALSE);
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Flushes any buffered output and restores the original {@code System.err} stream.
     * Called automatically by Spring when the application context is closed.
     */
    @Override
    public synchronized void flush() {
        flushStackTraceBuffer();
        if (rawLineBuffer.length() > 0) {
            String line = rawLineBuffer.toString();
            rawLineBuffer.setLength(0);
            emit(line);
        }
        super.flush();
    }

    @Override
    public void destroy() {
        flush();
        System.setErr(originalSystemErr);
    }
}
