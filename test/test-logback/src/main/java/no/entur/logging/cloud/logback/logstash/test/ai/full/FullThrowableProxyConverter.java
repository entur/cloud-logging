package no.entur.logging.cloud.logback.logstash.test.ai.full;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiText;

/**
 * Stack trace for the full file: the whole trace (all frames, causes and suppressed exceptions, in the order of <code>%ex</code>), but
 * on the same line as the message, so that every event is one line and <code>grep</code> returns a whole event. Empty if there is no
 * exception, otherwise it starts with a separator:
 * <code> | java.lang.IllegalStateException: message | at com.example.App.run(App.java:42) | Caused by: ...</code>.
 * Exception messages are on one line with control characters escaped (see {@link AiText}), so that for example a NUL in an exception
 * message does not make grep treat the whole file as binary.
 * <p>
 * The trace is written directly, not converted from the multi-line text of <code>%ex</code>: a trace can be tens of kilobytes, and
 * that would copy it once more.
 */
public class FullThrowableProxyConverter extends ThrowableProxyConverter {

    private static final String SEPARATOR = " | ";

    @Override
    public String convert(ILoggingEvent event) {
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(2048);
        append(builder, throwable, null, 0);
        return builder.toString();
    }

    private static void append(StringBuilder builder, IThrowableProxy throwable, String prefix, int depth) {
        builder.append(SEPARATOR);
        if (prefix != null) {
            builder.append(prefix);
        }
        if (throwable.isCyclic() || depth > 100) {
            builder.append("[CIRCULAR REFERENCE: ").append(throwable.getClassName()).append(']');
            return;
        }
        builder.append(throwable.getClassName());
        String message = throwable.getMessage();
        if (message != null) {
            builder.append(": ").append(AiText.singleLine(message));
        }
        StackTraceElementProxy[] frames = throwable.getStackTraceElementProxyArray();
        if (frames != null) {
            // the frames which the enclosing exception also has are left out, as in %ex
            int common = throwable.getCommonFrames();
            for (int i = 0; i < frames.length - common; i++) {
                builder.append(SEPARATOR).append(frames[i].getSTEAsString());
            }
            if (common > 0) {
                builder.append(SEPARATOR).append("... ").append(common).append(" common frames omitted");
            }
        }
        IThrowableProxy[] suppressed = throwable.getSuppressed();
        if (suppressed != null) {
            for (IThrowableProxy s : suppressed) {
                append(builder, s, "Suppressed: ", depth + 1);
            }
        }
        if (throwable.getCause() != null) {
            append(builder, throwable.getCause(), "Caused by: ", depth + 1);
        }
    }
}
