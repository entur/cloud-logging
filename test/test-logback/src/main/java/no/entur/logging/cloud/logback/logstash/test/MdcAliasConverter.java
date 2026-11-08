package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Like <code>%mdc</code>, but rendered as <code> [key=value, key=value]</code> (empty if there is no MDC), on a single line, and with
 * UUID values replaced by short aliases (<code>u1</code>, <code>u2</code>, ...) and hex id values (such as span ids) by <code>h1</code>, <code>h2</code>, ...;
 * the same value always gets the same alias within a JVM. See also {@link #aliasUuids(String)}, used for messages incl. request-response headers.
 * Meant for local AI mode only, to reduce the size of the log files. Disable aliasing with <code>-Dentur.logging.ai.alias=false</code>.
 * <p>
 * With option <code>compact</code> (for the compact file), the <code>logging.googleapis.com/</code> prefix is removed from keys, and such
 * entries are left out if their value equals another entry's value (by default the trace is the correlation id).
 */
public class MdcAliasConverter extends ClassicConverter {

    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    // e.g. span id (16 hex chars, leading zeros may be missing) and trace id (32 hex chars)
    private static final Pattern HEX_ID = Pattern.compile("(?=.*[a-f])(?=.*[0-9])[0-9a-f]{12,32}");
    private static final Pattern LINE_BREAKS = Pattern.compile("\\s*[\\r\\n]+\\s*");

    static final String GCP_KEY_PREFIX = "logging.googleapis.com/";

    private static final int MAX_ENTRIES = 10_000;

    private static final AtomicInteger UUID_COUNTER = new AtomicInteger();
    private static final AtomicInteger HEX_COUNTER = new AtomicInteger();
    private static final Map<String, String> ALIASES = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private boolean compact;

    @Override
    public void start() {
        compact = "compact".equals(getFirstOption());
        super.start();
    }

    /** Replaces all UUIDs in the text with aliases, unless disabled with {@link AiLoggingControl#AI_ALIAS_PROPERTY}. */
    public static String aliasUuids(String text) {
        if (text == null || text.indexOf('-') == -1 || !AiLoggingControl.isAliasEnabled()) {
            return text;
        }
        return UUID.matcher(text).replaceAll(m -> aliasOf(m.group()));
    }

    public static String aliasOf(String uuid) {
        return alias(uuid, "u", UUID_COUNTER);
    }

    private static String alias(String value, String prefix, AtomicInteger counter) {
        synchronized (ALIASES) {
            return ALIASES.computeIfAbsent(value, k -> prefix + counter.incrementAndGet());
        }
    }

    /** Aliases a value which is a hex id as a whole; UUIDs within the value are aliased as in messages. */
    static String aliasValue(String value) {
        if (value != null && AiLoggingControl.isAliasEnabled() && HEX_ID.matcher(value).matches()) {
            return alias(value, "h", HEX_COUNTER);
        }
        return aliasUuids(value);
    }

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return "";
        }
        Map<String, String> entries = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : mdc.entrySet()) {
            entries.put(e.getKey(), aliasValue(e.getValue()));
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
        return b.indexOf("\n") == -1 && b.indexOf("\r") == -1 ? b.toString() : LINE_BREAKS.matcher(b).replaceAll(" ");
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
