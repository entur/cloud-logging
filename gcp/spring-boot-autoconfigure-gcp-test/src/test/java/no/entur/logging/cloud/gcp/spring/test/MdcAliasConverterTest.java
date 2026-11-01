package no.entur.logging.cloud.gcp.spring.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.MdcAliasConverter;

public class MdcAliasConverterTest {

    private static final String A = "123e4567-e89b-12d3-a456-426614174000";
    private static final String B = "223e4567-e89b-12d3-a456-426614174000";

    private String convert(Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "m", null, null);
        event.setMDCPropertyMap(mdc);
        return new MdcAliasConverter().convert(event);
    }

    @Test
    public void uuidsAreAliasedConsistentlyAndOtherValuesKept() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("requestId", A);
        mdc.put("user", "john-doe");
        String first = convert(mdc);
        String alias = first.substring(first.indexOf('=') + 1, first.indexOf(','));
        assertThat(alias).matches("u\\d+");
        assertThat(first).endsWith("user=john-doe");

        mdc.put("requestId", B);
        assertThat(convert(mdc)).doesNotContain(alias + ",");
        mdc.put("requestId", A);
        assertThat(convert(mdc)).isEqualTo(first);
    }

    @Test
    public void emptyMdcGivesEmptyString() {
        assertThat(convert(new LinkedHashMap<>())).isEmpty();
    }
}
