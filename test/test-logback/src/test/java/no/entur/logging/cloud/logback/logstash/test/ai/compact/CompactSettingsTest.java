package no.entur.logging.cloud.logback.logstash.test.ai.compact;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

public class CompactSettingsTest {

    @Test
    public void truncatesNotInsideAnAlias() {
        assertThat(CompactSettings.truncate("body u1234 end", 7)).isEqualTo("body ...[truncated, 14 chars]");
        assertThat(CompactSettings.truncate("body", 7)).isEqualTo("body");
        assertThat(CompactSettings.truncate("body u1234 end", 0)).isEqualTo("body u1234 end");
    }
}
