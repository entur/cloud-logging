package no.entur.logging.cloud.logback.logstash.test.ai.common;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Like <code>%mdc</code>, but rendered as <code> [key=value, key=value]</code> (empty if there is no MDC), on a single line, and with
 * UUID and hex id values replaced by short aliases (see {@link AiAliases}).
 */
public abstract class AiMdcConverter extends ClassicConverter {

    private final boolean full;

    /** With <code>full</code>, the first time an alias is written it is followed by its value. */
    protected AiMdcConverter(boolean full) {
        this.full = full;
    }

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return "";
        }
        Map<String, String> entries = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : mdc.entrySet()) {
            entries.put(e.getKey(), AiAliases.aliasValue(e.getValue(), full));
        }
        entries = select(entries);
        StringBuilder b = new StringBuilder(" [");
        for (Map.Entry<String, String> e : entries.entrySet()) {
            if (b.length() > 2) {
                b.append(", ");
            }
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        b.append(']');
        return AiText.singleLine(b.toString());
    }

    /** The entries to write (values aliased), in order; all of them by default. */
    protected Map<String, String> select(Map<String, String> entries) {
        return entries;
    }
}
