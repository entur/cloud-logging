package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class CompactMdcConverterTest {

    private static final String A = "123e4567-e89b-12d3-a456-426614174000";
    private static final String B = "223e4567-e89b-12d3-a456-426614174000";

    private static String convert(Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "m", null, null);
        event.setMDCPropertyMap(mdc);
        CompactMdcConverter converter = new CompactMdcConverter();
        converter.start();
        return converter.convert(event);
    }

    @Test
    public void uuidsAreAliasedConsistentlyAndOtherValuesKept() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("requestId", A);
        mdc.put("user", "john-doe");
        String first = convert(mdc);
        assertThat(first).matches(" \\[requestId=u\\d+, user=john-doe]");
        String alias = first.substring(first.indexOf('=') + 1, first.indexOf(','));

        mdc.put("requestId", B);
        assertThat(convert(mdc)).doesNotContain(alias + ",");
        mdc.put("requestId", A);
        assertThat(convert(mdc)).isEqualTo(first);
    }

    @Test
    public void hexIdsAreAliased() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("spanId", "8524c7fac997d420");
        mdc.put("count", "123456789012");
        mdc.put("word", "deadbeefcafe");
        String converted = convert(mdc);
        assertThat(converted).matches(" \\[spanId=h\\d+, count=123456789012, word=deadbeefcafe]");
        assertThat(convert(mdc)).isEqualTo(converted);
    }

    @Test
    public void gcpKeyPrefixAndDuplicateGcpValuesAreRemoved() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("correlationId", A);
        mdc.put("logging.googleapis.com/trace", A);
        mdc.put("logging.googleapis.com/spanId", "8524c7fac997d420");
        String alias = AiAliases.aliasOf(A);

        assertThat(convert(mdc)).matches(" \\[correlationId=" + alias + ", spanId=h\\d+]");
    }
}
