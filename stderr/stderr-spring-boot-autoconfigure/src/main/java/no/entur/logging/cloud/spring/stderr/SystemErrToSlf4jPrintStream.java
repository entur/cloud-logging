package no.entur.logging.cloud.spring.stderr;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.DisposableBean;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
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
 * per-thread buffer whose owning thread has terminated or whose last append is older than
 * {@value #STALE_BUFFER_AGE_MS} ms.  This ensures that stack traces written by short-lived
 * threads (e.g. worker threads that exit after an exception) are always emitted even when no
 * subsequent write to {@code System.err} occurs on that thread.  The background thread is
 * started lazily on the first {@code printStackTrace} write to avoid consuming resources when
 * {@code System.err} is never used.
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
     * Background task that auto-flushes stale or orphaned per-thread buffers.  {@code null} until
     * the first {@code printStackTrace} write; started at most once via {@link #ensureSchedulerStarted()}.
     */
    private volatile ScheduledExecutorService staleFlusher;

    /** Guards one-time lazy creation of {@link #staleFlusher}. */
    private final Object schedulerLock = new Object();

    private final Logger logger;
    private final Level level;
    private final PrintStream originalSystemErr;

    // Buffer for the raw write() path (print(String), println(int), etc.) – guarded by 'this'
    private final StringBuilder rawLineBuffer = new StringBuilder(512);

    /**
     * Mutable container for a pending stack-trace accumulation buffer together with the
     * timestamp of the last line appended to it.  Access to {@code content} must be guarded by
     * {@code synchronized} on {@code this} instance.  {@code lastAppendNanos} is {@code volatile}
     * so the background flusher can read it without holding the lock (the staleness check is
     * approximate and a torn read would only cause a one-poll delay at worst).
     */
    private static final class PendingBuffer {
        final StringBuilder content = new StringBuilder(4096);
        volatile long lastAppendNanos = System.nanoTime();
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
                if (existingBuf.content.length() > 0 && isStackTraceBodyLine(l)) {
                    existingBuf.content.append('\n').append(l);
                    existingBuf.lastAppendNanos = System.nanoTime();
                    return;
                }
            }
        }

        if (isCalledFromPrintStackTrace()) {
            ensureSchedulerStarted();
            PendingBuffer buf = pendingBuffers.computeIfAbsent(currentThread, t -> new PendingBuffer());
            synchronized (buf) {
                if (buf.content.length() > 0) {
                    buf.content.append('\n');
                }
                buf.content.append(l);
                buf.lastAppendNanos = System.nanoTime();
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
                if (buf.content.length() == 0) {
                    return;
                }
                message = buf.content.toString();
                buf.content.setLength(0);
            }
            emit(message);
        }
    }

    /**
     * Flushes and removes all pending buffers across all threads.  Used by {@link #flush()} to
     * drain buffers that belong to threads other than the caller (e.g. completed threads whose
     * buffers were not yet picked up by the background flusher).
     */
    private void flushAllPendingBuffers() {
        for (Iterator<Map.Entry<Thread, PendingBuffer>> it = pendingBuffers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Thread, PendingBuffer> entry = it.next();
            PendingBuffer buf = entry.getValue();
            String message;
            synchronized (buf) {
                if (buf.content.length() == 0) {
                    it.remove();
                    continue;
                }
                message = buf.content.toString();
                buf.content.setLength(0);
            }
            it.remove();
            emit(message);
        }
    }

    /**
     * Background task: flushes any pending buffer whose owning thread has terminated or whose
     * last append is older than {@value #STALE_BUFFER_AGE_MS} ms.
     */
    private void flushStalePendingBuffers() {
        long now = System.nanoTime();
        for (Iterator<Map.Entry<Thread, PendingBuffer>> it = pendingBuffers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Thread, PendingBuffer> entry = it.next();
            Thread owner = entry.getKey();
            PendingBuffer buf = entry.getValue();
            boolean stale = !owner.isAlive() || (now - buf.lastAppendNanos) >= STALE_BUFFER_AGE_NS;
            if (!stale) {
                continue;
            }
            String message;
            synchronized (buf) {
                if (buf.content.length() == 0) {
                    it.remove();
                    continue;
                }
                message = buf.content.toString();
                buf.content.setLength(0);
            }
            it.remove();
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
     * Flushes any buffered output and restores the original {@code System.err} stream.
     * Called automatically by Spring when the application context is closed.
     */
    @Override
    public synchronized void flush() {
        flushCurrentThreadBuffer();
        flushAllPendingBuffers();
        if (rawLineBuffer.length() > 0) {
            String line = rawLineBuffer.toString();
            rawLineBuffer.setLength(0);
            emit(line);
        }
        super.flush();
    }

    @Override
    public void destroy() {
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
        flush();
        System.setErr(originalSystemErr);
    }
}
