package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Like <code>%mdc</code>, but rendered as <code> [key=value, key=value]</code> (empty if there is no MDC), on a single line, and with
 * UUID and hex id values replaced by short aliases (see {@link AiAliases}).
 * <p>
 * Without option (for the full file), the first time an alias is written to the full file it is followed by its value.
 * With option <code>compact</code> (for the compact file), the <code>logging.googleapis.com/</code> prefix is removed from keys, and such
 * entries are left out if their value equals another entry's value (by default the trace is the correlation id).
 */
public class MdcAliasConverter extends ClassicConverter {

    static final String GCP_KEY_PREFIX = "logging.googleapis.com/";

    private boolean compact;

    @Override
    public void start() {
        compact = "compact".equals(getFirstOption());
        super.start();
    }

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return "";
        }
        Map<String, String> entries = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : mdc.entrySet()) {
            entries.put(e.getKey(), AiAliases.aliasValue(e.getValue(), !compact));
        }
        if (compact) {
            entries = compact(entries);
        }
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

    private static Map<String, String> compact(Map<String, String> entries) {
        Map<String, String> otherValues = new HashMap<>();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            if (!e.getKey().startsWith(GCP_KEY_PREFIX)) {
                otherValues.put(e.getValue(), e.getKey());
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            String key = e.getKey();
            if (key.startsWith(GCP_KEY_PREFIX)) {
                if (otherValues.containsKey(e.getValue())) {
                    continue;
                }
                String shortKey = key.substring(GCP_KEY_PREFIX.length());
                if (!entries.containsKey(shortKey)) {
                    key = shortKey;
                }
            }
            result.put(key, e.getValue());
        }
        return result;
    }
}
