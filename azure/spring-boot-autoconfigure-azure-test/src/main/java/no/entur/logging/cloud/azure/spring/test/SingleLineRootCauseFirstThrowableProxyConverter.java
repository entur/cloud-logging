package no.entur.logging.cloud.azure.spring.test;

import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

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

        StringBuilder builder = new StringBuilder(multiLine.length() + 16);
        for (String line : multiLine.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
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
        return builder.toString();
    }
}
