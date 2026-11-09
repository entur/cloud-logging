package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class MdcAliasConverterTest {

    private static final String A = "123e4567-e89b-12d3-a456-426614174000";
    private static final String B = "223e4567-e89b-12d3-a456-426614174000";

    private String convert(Map<String, String> mdc, String option) {
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "m", null, null);
        event.setMDCPropertyMap(mdc);
        MdcAliasConverter converter = new MdcAliasConverter();
        if (option != null) {
            converter.setOptionList(List.of(option));
        }
        converter.start();
        return converter.convert(event);
    }

    @Test
    public void uuidsAreAliasedConsistentlyAndOtherValuesKept() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("requestId", A);
        mdc.put("user", "john-doe");
        String first = convert(mdc, "compact");
        assertThat(first).matches(" \\[requestId=u\\d+, user=john-doe]");
        String alias = first.substring(first.indexOf('=') + 1, first.indexOf(','));

        mdc.put("requestId", B);
        assertThat(convert(mdc, "compact")).doesNotContain(alias + ",");
        mdc.put("requestId", A);
        assertThat(convert(mdc, "compact")).isEqualTo(first);
    }

    @Test
    public void fullFileShowsTheValueTheFirstTime() {
        String uuid = java.util.UUID.randomUUID().toString();
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("correlationId", uuid);
        String alias = MdcAliasConverter.aliasOf(uuid);

        assertThat(convert(mdc, null)).isEqualTo(" [correlationId=" + alias + "(" + uuid + ")]");
        assertThat(convert(mdc, null)).isEqualTo(" [correlationId=" + alias + "]");
    }

    @Test
    public void hexIdsAreAliased() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("spanId", "8524c7fac997d420");
        mdc.put("count", "123456789012");
        mdc.put("word", "deadbeefcafe");
        String converted = convert(mdc, "compact");
        assertThat(converted).matches(" \\[spanId=h\\d+, count=123456789012, word=deadbeefcafe]");
        assertThat(convert(mdc, "compact")).isEqualTo(converted);
    }

    @Test
    public void compactRemovesGcpKeyPrefixAndDuplicateGcpValues() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("correlationId", A);
        mdc.put("logging.googleapis.com/trace", A);
        mdc.put("logging.googleapis.com/spanId", "8524c7fac997d420");
        String alias = MdcAliasConverter.aliasOf(A);

        assertThat(convert(mdc, "compact")).matches(" \\[correlationId=" + alias + ", spanId=h\\d+]");
        assertThat(convert(mdc, null)).contains("logging.googleapis.com/trace=" + alias);
    }

    @Test
    public void lineBreaksAreFlattened() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("text", "a\n  b");
        assertThat(convert(mdc, null)).isEqualTo(" [text=a b]");
    }

    @Test
    public void emptyMdcGivesEmptyString() {
        assertThat(convert(new LinkedHashMap<>(), null)).isEmpty();
    }
}
