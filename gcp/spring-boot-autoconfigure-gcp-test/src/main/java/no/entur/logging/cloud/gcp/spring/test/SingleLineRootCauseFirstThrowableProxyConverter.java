package no.entur.logging.cloud.gcp.spring.test;

import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.AiLoggingControl;

/**
 * Root-cause-first stack trace rendered on a single line, so that a trace can be found and read with grep.
 * Supports the same options as {@code %rEx}: depth followed by exclude patterns.
 * Output is empty if there is no exception, otherwise it starts with a separator, e.g.
 * {@code  | java.lang.RuntimeException: root | at a.B.c(B.java:1) | Wrapped by: java.lang.IllegalStateException: boom | ...}.
 */
public class SingleLineRootCauseFirstThrowableProxyConverter extends RootCauseFirstThrowableProxyConverter {

    private static final String SEPARATOR = " | ";

    @Override
    public String convert(ILoggingEvent event) {
        String multiLine = super.convert(event);
        if (multiLine == null || multiLine.isBlank()) {
            return "";
        }

        int maxLines = no.entur.logging.cloud.logback.logstash.test.AiLoggingControl.stackTraceMaxLines();
        int count = 0;
        int maxFrames = AiLoggingControl.stackTraceMaxLines();
        int maxCauses = AiLoggingControl.stackTraceMaxCauses();

        StringBuilder builder = new StringBuilder(multiLine.length() + 16);
        int causes = 0;
        int frames = 0;
        int omittedFrames = 0;
        boolean skipCause = false;
        for (String line : multiLine.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean frame = trimmed.startsWith("at ");
            boolean marker = trimmed.startsWith("[") || trimmed.startsWith("...");
            if (!frame && !marker) {
                // header of a new exception
                appendOmitted(builder, omittedFrames);
                omittedFrames = 0;
                frames = 0;
                causes++;
                if (maxCauses > 0 && causes > maxCauses) {
                    builder.append(SEPARATOR).append("[... more causes omitted]");
                    return builder.toString();
                }
            } else if (frame) {
                frames++;
                if (maxFrames > 0 && frames > maxFrames) {
                    omittedFrames++;
                    continue;
                }
            } else if (omittedFrames > 0) {
                continue;
            }
            // a lone "[N skipped]" marker belongs to the preceding frame
            if (trimmed.startsWith("[") && builder.length() > 0) {
                builder.append(' ');
            } else {
                builder.append(SEPARATOR);
            }
            builder.append(trimmed);
        }
        appendOmitted(builder, omittedFrames);
        return builder.toString();
    }

    private static void appendOmitted(StringBuilder builder, int omittedFrames) {
        if (omittedFrames > 0) {
            builder.append(SEPARATOR).append("[... ").append(omittedFrames).append(" frames omitted]");
        }
    }
}
