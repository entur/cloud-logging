package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Stack trace for the full file: like <code>%ex</code>, with control characters other than tab and line breaks escaped (see
 * {@link AiText}), so that for example a NUL in an exception message does not make grep treat the whole file as binary.
 */
public class FullThrowableProxyConverter extends ThrowableProxyConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String trace = super.convert(event);
        return trace == null || trace.isEmpty() ? trace : AiText.escapeControlCharacters(trace, true);
    }
}
