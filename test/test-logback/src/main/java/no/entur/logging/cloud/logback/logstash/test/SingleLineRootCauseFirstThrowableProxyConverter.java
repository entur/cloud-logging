package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;

import java.util.regex.Pattern;

/**
 * Root-cause-first stack trace rendered on a single line, so that a trace can be found and read with grep.
 * Supports the same options as {@code %rEx}: depth followed by exclude patterns (frames containing any of them are left out).
 * Frames per exception and the number of exceptions are limited by {@link AiLoggingControl#stackTraceMaxLines()} and
 * {@link AiLoggingControl#stackTraceMaxCauses()}; logback's own markers ({@code [N skipped]}, {@code ... N common frames omitted}) are dropped.
 * Output is empty if there is no exception, otherwise it starts with a separator, e.g.
 * {@code  | java.lang.RuntimeException: root | at a.B.c(B.java:1) | [... 3 frames omitted] | Wrapped by: java.lang.IllegalStateException: boom | ...}.
 */
public class SingleLineRootCauseFirstThrowableProxyConverter extends RootCauseFirstThrowableProxyConverter {

    private static final String SEPARATOR = " | ";

    private static final Pattern SKIPPED_SUFFIX = Pattern.compile("\\s*\\[\\d+ skipped]$");

    // a stack frame, 'at package.Class.method(Source.java:1)'; not a message line such as Jackson's 'at [Source: ...]'
    // class loader and module names may come first, as in at app//com.example.App.run(..) or at com.example.app@1.2.3+4/com.example.App.run(..)
    private static final Pattern FRAME = Pattern.compile("^at [\\w$./<>@+-]+\\(");

    @Override
    public String convert(ILoggingEvent event) {
        String multiLine = super.convert(event);
        if (multiLine == null || multiLine.isBlank()) {
            return "";
        }

        int maxFrames = AiLoggingControl.stackTraceMaxLines();
        int maxCauses = AiLoggingControl.stackTraceMaxCauses();

        StringBuilder builder = new StringBuilder(multiLine.length() + 16);
        int maxMessage = AiLoggingControl.messageMax();
        int causes = 0;
        int frames = 0;
        int omittedFrames = 0;
        int messageLength = 0;
        boolean messageCut = false;
        for (String line : multiLine.split("\\R")) {
            String trimmed = AiText.escapeControlCharacters(line.strip(), false);
            if (trimmed.isEmpty() || isLogbackMarker(trimmed)) {
                continue;
            }
            // logback indents suppressed exceptions and their causes; only the causes of the logged exception are at the start of a line
            boolean nested = Character.isWhitespace(line.charAt(0));
            if (FRAME.matcher(trimmed).lookingAt()) {
                frames++;
                if (maxFrames > 0 && frames > maxFrames) {
                    omittedFrames++;
                    continue;
                }
                builder.append(SEPARATOR).append(SKIPPED_SUFFIX.matcher(trimmed).replaceFirst(""));
            } else if (causes == 0 || isExceptionHeader(trimmed)) {
                appendOmitted(builder, omittedFrames);
                omittedFrames = 0;
                frames = 0;
                // suppressed exceptions (and their causes) are not causes: they do not count, so that the wrapping exceptions are kept
                if (!nested && !trimmed.startsWith(CoreConstants.SUPPRESSED)) {
                    causes++;
                }
                if (maxCauses > 0 && causes > maxCauses) {
                    builder.append(SEPARATOR).append("[... more causes omitted]");
                    return builder.toString();
                }
                builder.append(SEPARATOR).append(AiLoggingControl.truncate(trimmed, maxMessage));
                messageLength = trimmed.length();
                messageCut = maxMessage > 0 && messageLength > maxMessage;
            } else if (!messageCut) {
                // continuation of a multi-line exception message
                if (maxMessage > 0 && messageLength + 1 + trimmed.length() > maxMessage) {
                    builder.append(' ').append(AiText.cut(trimmed, Math.max(0, maxMessage - messageLength - 1))).append("...[truncated]");
                    messageCut = true;
                } else {
                    builder.append(' ').append(trimmed);
                    messageLength += 1 + trimmed.length();
                }
            }
        }
        appendOmitted(builder, omittedFrames);
        return builder.toString();
    }

    private static boolean isExceptionHeader(String line) {
        return line.startsWith(CoreConstants.WRAPPED_BY) || line.startsWith(CoreConstants.SUPPRESSED) || line.startsWith(CoreConstants.CAUSED_BY);
    }

    private static boolean isLogbackMarker(String line) {
        return (line.startsWith("[") && line.endsWith(" skipped]")) || (line.startsWith("... ") && line.endsWith(" omitted"));
    }

    private static void appendOmitted(StringBuilder builder, int omittedFrames) {
        if (omittedFrames > 0) {
            builder.append(SEPARATOR).append("[... ").append(omittedFrames).append(" frames omitted]");
        }
    }
}
