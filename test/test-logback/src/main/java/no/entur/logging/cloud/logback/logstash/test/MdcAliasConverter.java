package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Like <code>%mdc</code>, but rendered as <code> [key=value, key=value]</code> (empty if there is no MDC), on a single line, and with
 * UUID values replaced by short aliases (<code>u1</code>, <code>u2</code>, ...) and hex id values (such as span ids) by <code>h1</code>, <code>h2</code>, ...;
 * the same value always gets the same alias within a JVM (up to {@value #MAX_ENTRIES} values; later new values are left as they are).
 * See also {@link #aliasUuids(String, boolean)} for messages and bodies, and {@link #aliasIds(String, boolean)} for request-response headers.
 * Meant for local AI mode only, to reduce the size of the log files. Disable aliasing with <code>-Dentur.logging.ai.alias=false</code>.
 * <p>
 * Without option (for the full file), the first time an alias is written to the full file it is followed by its value,
 * <code>u7(123e4567-e89b-12d3-a456-426614174000)</code>, so that values can be found in both directions.
 * With option <code>compact</code> (for the compact file), the <code>logging.googleapis.com/</code> prefix is removed from keys, and such
 * entries are left out if their value equals another entry's value (by default the trace is the correlation id).
 */
public class MdcAliasConverter extends ClassicConverter {

    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    // e.g. span id (16 hex chars, leading zeros may be missing) and trace id (32 hex chars)
    private static final Pattern HEX_ID = Pattern.compile("(?=.*[a-f])(?=.*[0-9])[0-9a-f]{12,32}");
    // UUIDs and hex ids as tokens within a text (for example the trace and span ids in a traceparent header), in one pass so that
    // the parts of a UUID (or an alias followed by its value) are not taken for hex ids
    private static final Pattern UUID_OR_HEX_ID_TOKEN = Pattern.compile("(" + UUID.pattern() + ")"
            + "|(?<![0-9A-Za-z])(?=[0-9a-f]*[a-f])(?=[0-9a-f]*[0-9])[0-9a-f]{12,32}(?![0-9A-Za-z])");

    static final String GCP_KEY_PREFIX = "logging.googleapis.com/";

    static final int MAX_ENTRIES = 10_000;

    private static final AtomicInteger UUID_COUNTER = new AtomicInteger();
    private static final AtomicInteger HEX_COUNTER = new AtomicInteger();
    // never evicted, so that a value keeps its alias for the whole JVM; once full, new values are not aliased
    private static final Map<String, String> ALIASES = new HashMap<>(256);
    // aliases already written with their value to the full file
    private static final Set<String> ANNOUNCED = new HashSet<>(256);

    private boolean compact;

    @Override
    public void start() {
        compact = "compact".equals(getFirstOption());
        super.start();
    }

    /** Replaces all UUIDs in the text with aliases (for the compact file). */
    public static String aliasUuids(String text) {
        return aliasUuids(text, false);
    }

    /**
     * Replaces all UUIDs in the text with aliases, unless disabled with {@link AiLoggingControl#AI_ALIAS_PROPERTY}. With <code>full</code>,
     * an alias written for the first time is followed by its value.
     */
    public static String aliasUuids(String text, boolean full) {
        if (text == null || text.indexOf('-') == -1 || !AiLoggingControl.isAliasEnabled()) {
            return text;
        }
        return UUID.matcher(text).replaceAll(m -> java.util.regex.Matcher.quoteReplacement(alias(m.group(), "u", UUID_COUNTER, full)));
    }

    /** Like {@link #aliasUuids(String, boolean)}, and also hex ids (12 to 32 hex characters with a letter and a digit) within the text. */
    public static String aliasIds(String text, boolean full) {
        if (text == null || !AiLoggingControl.isAliasEnabled()) {
            return text;
        }
        return UUID_OR_HEX_ID_TOKEN.matcher(text).replaceAll(m -> java.util.regex.Matcher.quoteReplacement(m.group(1) != null
                ? alias(m.group(), "u", UUID_COUNTER, full)
                : alias(m.group(), "h", HEX_COUNTER, full)));
    }

    public static String aliasOf(String uuid) {
        return alias(uuid, "u", UUID_COUNTER, false);
    }

    private static String alias(String value, String prefix, AtomicInteger counter, boolean full) {
        synchronized (ALIASES) {
            String alias = ALIASES.get(value);
            if (alias == null) {
                if (ALIASES.size() >= MAX_ENTRIES) {
                    return value;
                }
                alias = prefix + counter.incrementAndGet();
                ALIASES.put(value, alias);
            }
            if (full && ANNOUNCED.add(alias)) {
                return alias + "(" + value + ")";
            }
            return alias;
        }
    }

    /** Aliases a value which is a hex id as a whole; UUIDs within the value are aliased as in messages. */
    static String aliasValue(String value, boolean full) {
        if (value != null && AiLoggingControl.isAliasEnabled() && HEX_ID.matcher(value).matches()) {
            return alias(value, "h", HEX_COUNTER, full);
        }
        return aliasUuids(value, full);
    }

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return "";
        }
        Map<String, String> entries = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : mdc.entrySet()) {
            entries.put(e.getKey(), aliasValue(e.getValue(), !compact));
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
