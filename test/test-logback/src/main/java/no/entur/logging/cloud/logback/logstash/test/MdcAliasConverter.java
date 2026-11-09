package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

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

    // UUIDs (8-4-4-4-12 hex digits, either case) and hex ids (a span id has 16 hex digits, a trace id 32) are found by hand: a UUID
    // regular expression took half of the time of the AI files for request-response events with JSON bodies (each hex digit in the
    // body starts a match attempt), and allocated a matcher for every message and MDC value
    private static final int UUID_LENGTH = 36;

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
        if (text == null || !AiLoggingControl.isAliasEnabled()) {
            return text;
        }
        return replaceUuids(text, uuid -> alias(uuid, "u", UUID_COUNTER, full));
    }

    /**
     * Like {@link #aliasUuids(String, boolean)}, and also hex ids within the text: a token (between characters which are not letters or
     * digits) of 12 to 32 lower-case hex characters with a letter and a digit, for example the trace and span ids in a traceparent header.
     * The parts of a UUID (or an alias followed by its value) are not taken for hex ids.
     */
    public static String aliasIds(String text, boolean full) {
        if (text == null || !AiLoggingControl.isAliasEnabled()) {
            return text;
        }
        return replaceIds(text, uuid -> alias(uuid, "u", UUID_COUNTER, full), hexId -> alias(hexId, "h", HEX_COUNTER, full));
    }

    /** Each UUID in the text replaced, from left to right; the text itself if there is none. Only looks at the text around each '-'. */
    static String replaceUuids(String text, UnaryOperator<String> replacement) {
        int dash = text.indexOf('-');
        if (dash == -1) {
            return text;
        }
        StringBuilder builder = null;
        int copied = 0;
        while (dash != -1) {
            // a UUID has its first '-' after 8 hex digits
            int start = dash - 8;
            if (start >= copied && isUuidAt(text, start)) {
                if (builder == null) {
                    builder = new StringBuilder(text.length());
                }
                builder.append(text, copied, start).append(replacement.apply(text.substring(start, start + UUID_LENGTH)));
                copied = start + UUID_LENGTH;
                dash = text.indexOf('-', copied);
            } else {
                dash = text.indexOf('-', dash + 1);
            }
        }
        return builder == null ? text : builder.append(text, copied, text.length()).toString();
    }

    /** Each UUID and hex id token in the text replaced, from left to right (a UUID first); the text itself if there is none. */
    static String replaceIds(String text, UnaryOperator<String> uuidReplacement, UnaryOperator<String> hexIdReplacement) {
        StringBuilder builder = null;
        int copied = 0;
        int i = 0;
        while (i < text.length()) {
            int end = -1;
            String replacement = null;
            if (isUuidAt(text, i)) {
                end = i + UUID_LENGTH;
                replacement = uuidReplacement.apply(text.substring(i, end));
            } else if (isLetterOrDigit(text.charAt(i)) && (i == 0 || !isLetterOrDigit(text.charAt(i - 1)))) {
                int tokenEnd = i;
                while (tokenEnd < text.length() && isLetterOrDigit(text.charAt(tokenEnd))) {
                    tokenEnd++;
                }
                if (isHexId(text, i, tokenEnd)) {
                    end = tokenEnd;
                    replacement = hexIdReplacement.apply(text.substring(i, end));
                }
            }
            if (replacement == null) {
                i++;
                continue;
            }
            if (builder == null) {
                builder = new StringBuilder(text.length());
            }
            builder.append(text, copied, i).append(replacement);
            copied = end;
            i = end;
        }
        return builder == null ? text : builder.append(text, copied, text.length()).toString();
    }

    private static boolean isUuidAt(String text, int start) {
        if (start < 0 || start + UUID_LENGTH > text.length() || text.charAt(start + 8) != '-') {
            return false;
        }
        for (int i = 0; i < UUID_LENGTH; i++) {
            char c = text.charAt(start + i);
            if (i == 8 || i == 13 || i == 18 || i == 23 ? c != '-' : !isHexDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isLetterOrDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
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

    /** 12 to 32 lower-case hex characters with at least one letter and one digit, such as a span or trace id. */
    static boolean isHexId(String value) {
        return isHexId(value, 0, value.length());
    }

    private static boolean isHexId(String text, int start, int end) {
        if (end - start < 12 || end - start > 32) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                digit = true;
            } else if (c >= 'a' && c <= 'f') {
                letter = true;
            } else {
                return false;
            }
        }
        return letter && digit;
    }

    /** Aliases a value which is a hex id as a whole; UUIDs within the value are aliased as in messages. */
    static String aliasValue(String value, boolean full) {
        if (value != null && AiLoggingControl.isAliasEnabled() && isHexId(value)) {
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
