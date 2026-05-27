package no.entur.logging.cloud.spring.stderr;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.DisposableBean;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A {@link PrintStream} that intercepts all output written to {@code System.err} and forwards it
 * to an SLF4J {@link Logger}.
 *
 * <h2>Detection strategy</h2>
 * <p>{@link Throwable#printStackTrace(PrintStream)} exclusively uses {@link #println(Object)} –
 * one call per line – routing the output through its internal {@code WrappedPrintStream}.
 * This class overrides {@link #println(Object)} and uses {@link StackWalker} to check whether
 * {@code Throwable.printStackTrace} is present in the current thread's call stack.  When it is,
 * lines are accumulated in a per-thread buffer; the complete trace is emitted as a single SLF4J
 * log statement once the next non-{@code printStackTrace} write arrives or when {@link #flush()}
 * is called.  Every other {@link #println} call is forwarded immediately without buffering.
 *
 * <h2>Thread safety</h2>
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
    private static final ThreadLocal<StringBuilder> STACK_TRACE_BUFFER = ThreadLocal.withInitial(() -> new StringBuilder(4096));

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
    private final StringBuilder rawLineBuffer = new StringBuilder(512);

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
     *
     * <h4>Stack-walk optimisation</h4>
     * <p>When this thread is already accumulating a stack trace and the incoming line is
     * unambiguously a stack-trace body line (a frame, truncated-frame indicator, or cause/
     * suppressed header), the stack walk is skipped entirely and the line is appended to the
     * existing buffer.  The walk is only performed for the first line of a potential new trace
     * or when the line content does not conclusively identify it as a continuation.
     */
    @Override
    public void println(Object x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(String l) {
        if(l == null) {
            flushStackTraceBuffer();
            emit("null");
            return;
        }
        StringBuilder stackTraceBuffer = STACK_TRACE_BUFFER.get();

        // Fast path: already accumulating a trace on this thread and the line is an
        // unambiguous body line – skip the (relatively expensive) stack walk.
        if (!stackTraceBuffer.isEmpty() && isStackTraceBodyLine(l)) {
            stackTraceBuffer.append('\n').append(l);
            return;
        }

        if (isCalledFromPrintStackTrace()) {
            if (!stackTraceBuffer.isEmpty()) {
                stackTraceBuffer.append('\n');
            }
            stackTraceBuffer.append(l);
        } else {
            flushStackTraceBuffer(stackTraceBuffer);
            emit(l);
        }
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
            int b = buf[i] & 0xff;
            if (b == '\n') {
                String line = rawLineBuffer.toString();
                rawLineBuffer.setLength(0);
                emit(line);
            } else if (b != '\r') {
                rawLineBuffer.append((char) b);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stack-trace detection helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when {@code java.lang.Throwable.printStackTrace} appears at the
     * expected fixed depth in the current thread's call stack.
     * The call chain from here to {@code Throwable.printStackTrace} is:
     * {@code isCalledFromPrintStackTrace} → {@code println(Object)} →
     * {@code Throwable$WrappedPrintStream.println} → {@code Throwable.printStackTrace}.
     * Limiting the walk to a small, fixed depth avoids traversing the full stack.
     */
    private boolean isCalledFromPrintStackTrace() {
        return STACK_WALKER.walk(frames ->
            frames.limit(5).anyMatch(f ->
                "java.lang.Throwable".equals(f.getClassName()) &&
                "printStackTrace".equals(f.getMethodName())
            )
        );
    }

    /**
     * Returns {@code true} for lines that are unambiguously part of a stack-trace body as
     * produced by {@link Throwable#printStackTrace(PrintStream)}:
     * <ul>
     *   <li>{@code "\tat com.example.Foo.bar(Foo.java:42)"} – ordinary stack frame</li>
     *   <li>{@code "\t... 5 more"}                         – truncated frames indicator</li>
     *   <li>{@code "Caused by: java.lang.RuntimeException"} – cause chain header</li>
     *   <li>{@code "\tSuppressed: java.lang.Exception"}     – suppressed exception header</li>
     * </ul>
     * Used as a fast pre-check to avoid a stack walk when we are already accumulating a trace.
     */
    private static boolean isStackTraceBodyLine(String line) {
        return line.startsWith("\tat ")
            || line.startsWith("\t...")
            || line.startsWith("Caused by: ")
            || line.startsWith("\tSuppressed: ");
    }

    /**
     * Emits the accumulated per-thread stack-trace buffer as a single log statement and resets
     * the buffer.  A no-op when the buffer is empty.
     */
    private void flushStackTraceBuffer() {
        StringBuilder buf = STACK_TRACE_BUFFER.get();
        flushStackTraceBuffer(buf);
    }

    private void flushStackTraceBuffer(StringBuilder buf) {
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
        flushStackTraceBuffer(rawLineBuffer);
        super.flush();
    }

    @Override
    public void destroy() {
        flush();
        System.setErr(originalSystemErr);
    }
}
