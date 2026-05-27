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
 * <p>Output is processed line by line. When a sequence of lines matching the pattern produced by
 * {@link Throwable#printStackTrace()} is detected (i.e., lines beginning with {@code "\tat "},
 * {@code "Caused by: "}, etc.) the entire stack trace is accumulated and emitted as a single log
 * statement so that cloud log viewers can correlate all lines of an exception together.
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

    private final Logger logger;
    private final Level level;
    private final PrintStream originalSystemErr;

    // Characters accumulated for the line currently being written
    private final StringBuilder currentLine = new StringBuilder();

    // Lines accumulated while collecting a stack trace
    private final StringBuilder stackTraceBuffer = new StringBuilder();

    // The last completed line that has not yet been emitted (may turn out to be the
    // first line of an upcoming stack trace, so we keep it pending until we know)
    private String pendingLine = null;

    // Whether we are currently collecting lines that belong to a stack trace
    private boolean inStackTrace = false;

    /**
     * @param logger           the SLF4J logger to write forwarded output to
     * @param level            the log level to use
     * @param originalSystemErr the original {@code System.err} stream to restore on {@link #destroy()}
     */
    public SystemErrToSlf4jPrintStream(Logger logger, Level level, PrintStream originalSystemErr) {
        super(OutputStream.nullOutputStream());
        this.logger = logger;
        this.level = level;
        this.originalSystemErr = originalSystemErr;
    }

    // -------------------------------------------------------------------------
    // PrintStream overrides – intercept all byte-level writes
    // -------------------------------------------------------------------------

    @Override
    public synchronized void write(int b) {
        if (b == '\n') {
            String line = currentLine.toString();
            currentLine.setLength(0);
            processCompletedLine(line);
        } else if (b != '\r') {
            currentLine.append((char) b);
        }
    }

    @Override
    public synchronized void write(byte[] buf, int off, int len) {
        for (int i = off; i < off + len; i++) {
            write(buf[i] & 0xff);
        }
    }

    @Override
    public synchronized void flush() {
        if (currentLine.length() > 0) {
            processCompletedLine(currentLine.toString());
            currentLine.setLength(0);
        }
        emitPending();
        super.flush();
    }

    // -------------------------------------------------------------------------
    // Stack-trace detection and line routing
    // -------------------------------------------------------------------------

    /**
     * Routes a completed (newline-terminated) line either into the stack-trace accumulator or
     * emits it immediately as a standalone log statement.
     */
    private void processCompletedLine(String line) {
        if (isStackTraceContinuationLine(line)) {
            if (!inStackTrace) {
                // Retroactively treat the pending line as the exception header
                inStackTrace = true;
                stackTraceBuffer.setLength(0);
                if (pendingLine != null) {
                    stackTraceBuffer.append(pendingLine);
                    pendingLine = null;
                }
            }
            if (stackTraceBuffer.length() > 0) {
                stackTraceBuffer.append('\n');
            }
            stackTraceBuffer.append(line);
        } else {
            if (inStackTrace) {
                // The stack trace has ended – flush the accumulated lines as one log entry
                emitStackTrace();
                inStackTrace = false;
                stackTraceBuffer.setLength(0);
            } else if (pendingLine != null) {
                // Emit the previously held line – it was not part of a stack trace
                emit(pendingLine);
            }
            pendingLine = line;
        }
    }

    /**
     * Returns {@code true} for lines that are part of a stack trace produced by
     * {@link Throwable#printStackTrace()}.
     *
     * <ul>
     *   <li>{@code "\tat com.example.Foo.bar(Foo.java:42)"} – ordinary stack frame</li>
     *   <li>{@code "\t... 5 more"}                         – truncated frames indicator</li>
     *   <li>{@code "Caused by: java.lang.RuntimeException"} – exception cause chain</li>
     *   <li>{@code "\tSuppressed: java.lang.Exception"}     – suppressed exception</li>
     * </ul>
     */
    private boolean isStackTraceContinuationLine(String line) {
        if (line.startsWith("\tat "))         return true;  // normal stack frame
        if (line.startsWith("\t..."))         return true;  // "... N more"
        if (line.startsWith("Caused by: "))  return true;  // cause chain
        if (line.startsWith("\tSuppressed: ")) return true; // suppressed exception
        return false;
    }

    /** Emits any content that is still buffered (called at flush/destroy time). */
    private void emitPending() {
        if (inStackTrace) {
            emitStackTrace();
            inStackTrace = false;
            stackTraceBuffer.setLength(0);
        } else if (pendingLine != null) {
            emit(pendingLine);
            pendingLine = null;
        }
    }

    private void emitStackTrace() {
        String message = stackTraceBuffer.toString();
        if (!message.isEmpty()) {
            emit(message);
        }
    }

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
    public void destroy() {
        flush();
        System.setErr(originalSystemErr);
    }
}
