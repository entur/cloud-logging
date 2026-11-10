package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class FullMdcConverterTest {

    private static String convert(Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "m", null, null);
        event.setMDCPropertyMap(mdc);
        FullMdcConverter converter = new FullMdcConverter();
        converter.start();
        return converter.convert(event);
    }

    @Test
    public void valueIsShownTheFirstTime() {
        String uuid = UUID.randomUUID().toString();
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("correlationId", uuid);
        String alias = AiAliases.aliasOf(uuid);

        assertThat(convert(mdc)).isEqualTo(" [correlationId=" + alias + "(" + uuid + ")]");
        assertThat(convert(mdc)).isEqualTo(" [correlationId=" + alias + "]");
    }

    @Test
    public void gcpEntriesAreKept() {
        String uuid = UUID.randomUUID().toString();
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("correlationId", uuid);
        mdc.put("logging.googleapis.com/trace", uuid);
        String alias = AiAliases.aliasOf(uuid);

        assertThat(convert(mdc)).contains("logging.googleapis.com/trace=" + alias);
    }

    @Test
    public void lineBreaksAreFlattened() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("text", "a\n  b");
        assertThat(convert(mdc)).isEqualTo(" [text=a b]");
    }

    @Test
    public void emptyMdcGivesEmptyString() {
        assertThat(convert(new LinkedHashMap<>())).isEmpty();
    }
}
