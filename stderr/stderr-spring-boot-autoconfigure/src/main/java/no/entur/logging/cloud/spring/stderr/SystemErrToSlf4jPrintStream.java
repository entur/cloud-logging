package no.entur.logging.cloud.spring.stderr;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.DisposableBean;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
 * <h2>Automatic flush of pending buffers</h2>
 * <p>A background daemon thread polls every {@value #FLUSHER_INTERVAL_MS} ms and flushes any
 * per-thread buffer lines that are older than {@value #STALE_BUFFER_AGE_MS} ms or whose owning
 * thread has terminated.  Each line is stored with its own append timestamp, so consecutive
 * {@code printStackTrace} calls on the same thread are flushed independently: once the lines
 * from the first trace grow stale they are emitted as a single log statement even while newer
 * lines from a second trace are still accumulating in the same buffer.
 *
 * <h2>Thread safety</h2>
 * <p>Stack-trace accumulation uses a {@link ConcurrentHashMap} keyed on the owning
 * {@link Thread}, so concurrent {@code printStackTrace} calls from different threads do not
 * interfere with each other.  Access to each individual {@link PendingBuffer} is guarded by
 * {@code synchronized} on that buffer object.
 *
 * <p>Instances of this class save the previous {@code System.err} reference and restore it when
 * the Spring application context is closed (via the {@link DisposableBean} contract).
 */
public class SystemErrToSlf4jPrintStream extends PrintStream implements DisposableBean {

    /** How often the background flusher wakes up (milliseconds). */
    static final long FLUSHER_INTERVAL_MS = 100L;

    /** A pending buffer older than this (in nanoseconds) is considered stale and flushed. */
    static final long STALE_BUFFER_AGE_MS = 200L;
    private static final long STALE_BUFFER_AGE_NS = STALE_BUFFER_AGE_MS * 1_000_000L;

    /** How long {@link #destroy()} waits for the background flusher to finish its last run (milliseconds). */
    static final long SHUTDOWN_TIMEOUT_MS = FLUSHER_INTERVAL_MS * 2;

    /**
     * Guard against infinite recursion that would occur if the SLF4J backend itself wrote to
     * {@code System.err} (e.g. during its own initialisation or error reporting).
     */
    private static final ThreadLocal<Boolean> LOGGING_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Lazy {@link StackWalker} used to determine whether the current {@link #println(Object)}
     * call originates from {@link Throwable#printStackTrace}.  The walker stops as soon as a
     * matching frame is found, making the check inexpensive for the common case.
     */
    private static final StackWalker STACK_WALKER = StackWalker.getInstance();

    /**
     * Per-thread accumulator for lines produced by {@link Throwable#printStackTrace(PrintStream)}.
     * Keyed on the owning {@link Thread}; access to each value is guarded by {@code synchronized}
     * on the {@link PendingBuffer} instance.
     */
    private final ConcurrentHashMap<Thread, PendingBuffer> pendingBuffers = new ConcurrentHashMap<>();

    /**
     * Background task that auto-flushes stale or orphaned per-thread buffers.  Started
     * unconditionally in the constructor via {@link #ensureSchedulerStarted()}; subsequent
     * calls to {@code ensureSchedulerStarted()} are no-ops.
     */
    private volatile ScheduledExecutorService staleFlusher;

    /** Guards one-time lazy creation of {@link #staleFlusher}. */
    private final Object schedulerLock = new Object();

    private final Logger logger;
    private final Level level;
    private final PrintStream originalSystemErr;
    private volatile boolean destroying;

    // Buffer for the raw write() path (print(String), println(int), etc.) – guarded by 'this'
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private final ByteArrayOutputStream rawLineBuffer = new ByteArrayOutputStream(512);

    /**
     * One line produced by {@link Throwable#printStackTrace(PrintStream)}, together with the
     * nanosecond timestamp at which it was appended to the buffer.
     */
    private static final class TimestampedLine {
        final String line;
        final long appendNanos;

        TimestampedLine(String line, long appendNanos) {
            this.line = line;
            this.appendNanos = appendNanos;
        }
    }

    /**
     * Mutable container for lines accumulated from one or more consecutive
     * {@link Throwable#printStackTrace} calls on the same thread.  Each line carries its own
     * append timestamp so that the background flusher can emit only the lines that are old
     * enough, leaving any still-active trace lines for a subsequent poll.  Access to
     * {@code lines} must be guarded by {@code synchronized} on {@code this} instance.
     */
    private static final class PendingBuffer {
        final List<TimestampedLine> lines = new ArrayList<>();
    }

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
        ensureSchedulerStarted();
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
        if (destroying) {
            originalSystemErr.println(l);
            return;
        }
        if (l == null) {
            flushCurrentThreadBuffer();
            emit("null");
            return;
        }

        Thread currentThread = Thread.currentThread();
        PendingBuffer existingBuf = pendingBuffers.get(currentThread);

        // Fast path: already accumulating a trace on this thread and the line is an
        // unambiguous body line – skip the (relatively expensive) stack walk.
        if (existingBuf != null) {
            synchronized (existingBuf) {
                if (!existingBuf.lines.isEmpty() && isStackTraceBodyLine(l)) {
                    existingBuf.lines.add(new TimestampedLine(l, System.nanoTime()));
                    return;
                }
            }
        }

        if (isCalledFromPrintStackTrace()) {
            ensureSchedulerStarted();
            PendingBuffer buf = pendingBuffers.computeIfAbsent(currentThread, t -> new PendingBuffer());
            String previousTrace = null;
            synchronized (buf) {
                // A non-body line while the buffer is non-empty is the header of a new
                // printStackTrace call.  Flush the previous trace immediately so each call
                // produces its own log event without waiting for the stale-age timeout.
                if (!buf.lines.isEmpty() && !isStackTraceBodyLine(l)) {
                    previousTrace = buildMessage(buf.lines, buf.lines.size());
                    buf.lines.clear();
                }
                buf.lines.add(new TimestampedLine(l, System.nanoTime()));
            }
            if (previousTrace != null) {
                emit(previousTrace);
            }
        } else {
            flushCurrentThreadBuffer();
            emit(l);
        }
    }

    // -------------------------------------------------------------------------
    // Raw write fallback – handles print(String), println(int/long/…), etc.
    // These methods are never called by Throwable.printStackTrace.
    // -------------------------------------------------------------------------

    @Override
    public synchronized void write(int b) {
        if (destroying) {
            originalSystemErr.write(b);
            return;
        }
        if (b == '\n') {
            flushCurrentThreadBuffer();
            String line = rawLineBuffer.toString(CHARSET);
            rawLineBuffer.reset();
            emit(line);
        } else if (b != '\r') {
            rawLineBuffer.write(b);
        }
    }

    @Override
    public synchronized void write(byte[] buf, int off, int len) {
        if (destroying) {
            originalSystemErr.write(buf, off, len);
            return;
        }
        for (int i = off; i < off + len; i++) {
            int b = buf[i] & 0xff;
            if (b == '\n') {
                flushCurrentThreadBuffer();
                String line = rawLineBuffer.toString(CHARSET);
                rawLineBuffer.reset();
                emit(line);
            } else if (b != '\r') {
                rawLineBuffer.write(b);
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
        int length = line.length();
        if (length < 2) {
            return false;
        }
        if (line.charAt(0) == '\t') {
            if (length >= 5 && line.charAt(1) == 'a' && line.charAt(2) == 't' && line.charAt(3) == ' ') {
                return true;
            }
            if (length >= 4 && line.charAt(1) == '.' && line.charAt(2) == '.' && line.charAt(3) == '.') {
                return true;
            }
            return length >= 13 && line.startsWith("\tSuppressed: ");
        }
        return length >= 11 && line.startsWith("Caused by: ");
    }

    // -------------------------------------------------------------------------
    // Buffer management
    // -------------------------------------------------------------------------

    /**
     * Flushes and removes the pending buffer for the current thread, if one exists and is
     * non-empty.  This is called when a non-{@code printStackTrace} write arrives, signalling
     * that the previous trace (if any) is complete.
     */
    private void flushCurrentThreadBuffer() {
        PendingBuffer buf = pendingBuffers.remove(Thread.currentThread());
        if (buf != null) {
            String message;
            synchronized (buf) {
                if (buf.lines.isEmpty()) {
                    return;
                }
                message = buildMessage(buf.lines, buf.lines.size());
                buf.lines.clear();
            }
            emit(message);
        }
    }

    /**
     * Flushes and removes all pending buffers across all threads.  Used by {@link #destroy()}
     * to drain every remaining buffer after writes have been gated by {@code destroying = true},
     * guaranteeing that no new lines can be added concurrently.
     */
    private void flushAllPendingBuffers() {
        for (Iterator<Map.Entry<Thread, PendingBuffer>> it = pendingBuffers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Thread, PendingBuffer> entry = it.next();
            PendingBuffer buf = entry.getValue();
            String message;
            synchronized (buf) {
                if (buf.lines.isEmpty()) {
                    it.remove();
                    continue;
                }
                message = buildMessage(buf.lines, buf.lines.size());
                buf.lines.clear();
            }
            it.remove();
            emit(message);
        }
    }

    /**
     * Background task: for each pending buffer, emits all leading lines whose individual
     * append timestamp is older than {@value #STALE_BUFFER_AGE_MS} ms, or all lines if the
     * owning thread has terminated.  Lines are appended in chronological order, so the oldest
     * lines are always at the front of the list; scanning stops at the first line that is still
     * within the staleness window.  This allows the first of two consecutive
     * {@code printStackTrace} calls on the same thread to be flushed independently once its
     * lines age out, even while newer lines from the second call are still accumulating.
     */
    private void flushStalePendingBuffers() {
        long now = System.nanoTime();
        for (Iterator<Map.Entry<Thread, PendingBuffer>> it = pendingBuffers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Thread, PendingBuffer> entry = it.next();
            Thread owner = entry.getKey();
            PendingBuffer buf = entry.getValue();
            boolean threadDead = !owner.isAlive();
            String message;
            synchronized (buf) {
                if (buf.lines.isEmpty()) {
                    it.remove();
                    continue;
                }
                // Count consecutive lines from the front that are old enough to flush.
                int flushCount = 0;
                for (TimestampedLine tl : buf.lines) {
                    if (threadDead || (now - tl.appendNanos) >= STALE_BUFFER_AGE_NS) {
                        flushCount++;
                    } else {
                        break; // Lines are chronological; stop at the first fresh line.
                    }
                }
                if (flushCount == 0) {
                    continue;
                }
                message = buildMessage(buf.lines, flushCount);
                buf.lines.subList(0, flushCount).clear();
                if (buf.lines.isEmpty()) {
                    it.remove();
                }
            }
            emit(message);
        }
    }

    /**
     * Joins the first {@code count} lines from {@code lines} into a single newline-delimited
     * string suitable for passing to {@link #emit(String)}.
     */
    private static String buildMessage(List<TimestampedLine> lines, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i).line);
        }
        return sb.toString();
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
     * Starts the background stale-buffer flusher the first time it is needed.  The flusher is
     * started at most once; subsequent calls after the first are no-ops.
     */
    private void ensureSchedulerStarted() {
        if (staleFlusher == null) {
            synchronized (schedulerLock) {
                if (staleFlusher == null) {
                    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "stderr-stale-flusher");
                        t.setDaemon(true);
                        return t;
                    });
                    executor.scheduleAtFixedRate(() -> {
                        try {
                            flushStalePendingBuffers();
                        } catch (Exception ignored) {
                            // Never let an exception cancel the scheduled task
                        }
                    }, FLUSHER_INTERVAL_MS, FLUSHER_INTERVAL_MS, TimeUnit.MILLISECONDS);
                    staleFlusher = executor; // volatile write - makes the executor visible to all threads
                }
            }
        }
    }

    /**
     * Flushes any buffered output for the <em>current thread</em> and any stale or orphaned
     * per-thread buffers.  Active buffers owned by other threads that are still in the middle
     * of a {@code printStackTrace} call are intentionally left untouched to avoid splitting
     * a single exception into multiple log events.  Call {@link #flushAllPendingBuffers()}
     * only after writes have been gated (e.g. during {@link #destroy()}).
     */
    @Override
    public synchronized void flush() {
        flushCurrentThreadBuffer();
        flushStalePendingBuffers();
        if (rawLineBuffer.size() > 0) {
            String line = rawLineBuffer.toString(CHARSET);
            rawLineBuffer.reset();
            emit(line);
        }
        super.flush();
    }

    /**
     * Gates new writes, drains all pending buffers, and restores the original
     * {@code System.err} stream.  Called automatically by Spring when the
     * application context is closed.
     */
    @Override
    public void destroy() {
        destroying = true;
        ScheduledExecutorService flusher = staleFlusher;
        if (flusher != null) {
            flusher.shutdown();
            try {
                // Wait for any in-flight flush task to complete before doing the final drain.
                flusher.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // flush() only drains current-thread and stale buffers; once destroying=true gates
        // all new writes it is safe to drain every remaining buffer unconditionally.
        flush();
        flushAllPendingBuffers();
        System.setErr(originalSystemErr);
    }
}
