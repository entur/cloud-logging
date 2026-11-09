package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Like <code>%ex</code>, with control characters other than tab and line breaks escaped (see {@link AiText}), so that for example a
 * NUL in an exception message does not make grep treat the whole file as binary. For the full file.
 */
public class EscapedThrowableProxyConverter extends ThrowableProxyConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String trace = super.convert(event);
        return trace == null || trace.isEmpty() ? trace : AiText.escapeControlCharacters(trace, true);
    }
}
