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

    @Override
    public String convert(ILoggingEvent event) {
        String multiLine = super.convert(event);
        if (multiLine == null || multiLine.isBlank()) {
            return "";
        }

        int maxFrames = AiLoggingControl.stackTraceMaxLines();
        int maxCauses = AiLoggingControl.stackTraceMaxCauses();

        StringBuilder builder = new StringBuilder(multiLine.length() + 16);
        int causes = 0;
        int frames = 0;
        int omittedFrames = 0;
        for (String line : multiLine.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || isLogbackMarker(trimmed)) {
                continue;
            }
            if (trimmed.startsWith("at ")) {
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
                causes++;
                if (maxCauses > 0 && causes > maxCauses) {
                    builder.append(SEPARATOR).append("[... more causes omitted]");
                    return builder.toString();
                }
                builder.append(SEPARATOR).append(trimmed);
            } else {
                // continuation of a multi-line exception message
                builder.append(' ').append(trimmed);
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
