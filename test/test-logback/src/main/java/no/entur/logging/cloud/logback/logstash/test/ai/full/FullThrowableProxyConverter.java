package no.entur.logging.cloud.logback.logstash.test.ai.full;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiText;

/**
 * Stack trace for the full file: the whole trace as <code>%ex</code> prints it (all frames and causes, in that order), but on the same
 * line as the message, so that every event is one line and <code>grep</code> returns a whole event. Empty if there is no exception,
 * otherwise it starts with a separator:
 * <code> | java.lang.IllegalStateException: message | at com.example.App.run(App.java:42) | Caused by: ...</code>.
 * Control characters are escaped (see {@link AiText}), so that for example a NUL in an exception message does not make grep treat
 * the whole file as binary.
 */
public class FullThrowableProxyConverter extends ThrowableProxyConverter {

    private static final String SEPARATOR = " | ";

    @Override
    public String convert(ILoggingEvent event) {
        String trace = super.convert(event);
        if (trace == null || trace.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(trace.length() + 64);
        for (String line : trace.lines().toList()) {
            String trimmed = AiText.singleLine(line.strip());
            if (!trimmed.isEmpty()) {
                builder.append(SEPARATOR).append(trimmed);
            }
        }
        return builder.toString();
    }
}
