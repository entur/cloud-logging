package no.entur.logging.cloud.logback.logstash.test.ai.compact;

import no.entur.logging.cloud.logback.logstash.test.ai.common.AiMdcConverter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MDC for the compact file (see {@link AiMdcConverter}): the <code>logging.googleapis.com/</code> prefix is removed from keys, and such
 * entries are left out if their value equals another entry's value (by default the trace is the correlation id).
 */
public class CompactMdcConverter extends AiMdcConverter {

    static final String GCP_KEY_PREFIX = "logging.googleapis.com/";

    public CompactMdcConverter() {
        super(false);
    }

    @Override
    protected Map<String, String> select(Map<String, String> entries) {
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
