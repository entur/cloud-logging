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

    @Test
    public void messagesAreSqueezedBeforeTheyAreCutButBodiesAreNot() {
        // squeezed only if too long; then cut if still too long, with the length of the original
        assertThat(CompactSettings.truncateSqueezed("a ===== b", 9)).isEqualTo("a ===== b");
        assertThat(CompactSettings.truncateSqueezed("a ===== b", 8)).isEqualTo("a === b");
        assertThat(CompactSettings.truncateSqueezed("a =====    b c d", 6)).isEqualTo("a === ...[truncated, 16 chars]");
        // a body is data
        assertThat(CompactSettings.truncate("a ===== b", 8)).isEqualTo("a ===== ...[truncated, 9 chars]");
    }
}
