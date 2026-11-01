package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Like <code>%mdc</code>, but UUID values (see also {@link #aliasUuids(String)}, used for messages incl. request-response headers) are replaced by short aliases (<code>u1</code>, <code>u2</code>, ...), the same UUID
 * always getting the same alias within a JVM. Meant for local AI mode only, to reduce the size of the log files.
 * Disable with <code>-Dentur.logging.ai.mdc.alias=false</code>.
 */
public class MdcAliasConverter extends ClassicConverter {

    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final int MAX_ENTRIES = 10_000;

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final Map<String, String> ALIASES = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    /** Replaces all UUIDs in the text with aliases, unless disabled with {@link AiLoggingControl#AI_MDC_ALIAS_PROPERTY}. */
    public static String aliasUuids(String text) {
        if (text == null || text.indexOf('-') == -1 || "false".equalsIgnoreCase(System.getProperty(AiLoggingControl.AI_MDC_ALIAS_PROPERTY))) {
            return text;
        }
        return UUID.matcher(text).replaceAll(m -> aliasOf(m.group()));
    }

    public static String aliasOf(String uuid) {
        synchronized (ALIASES) {
            return ALIASES.computeIfAbsent(uuid, k -> "u" + COUNTER.incrementAndGet());
        }
    }

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : mdc.entrySet()) {
            if (b.length() > 0) {
                b.append(", ");
            }
            String value = e.getValue();
            value = aliasUuids(value);
            b.append(e.getKey()).append('=').append(value);
        }
        return b.toString();
    }
}
