package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

public class AiTextTest {

    @Test
    public void singleLineBodyKeepsJsonContentAndSeparatesOtherTokens() {
        assertThat(AiText.singleLineBody("{\n  \"id\" : 1,\n  \"id\" : 1.10, \"s\": \"a \\\" b\\u00e9\"\n}\n", true))
                .isEqualTo("{\"id\":1,\"id\":1.10,\"s\":\"a \\\" b\\u00e9\"}");
        // not valid JSON: only line breaks are replaced, by a space
        assertThat(AiText.singleLineBody("{\"a\": tru\n e}", true)).isEqualTo("{\"a\": tru e}");
        assertThat(AiText.singleLineBody("<item\n   id=\"1\"/>\n", false)).isEqualTo("<item id=\"1\"/>");
    }

    @Test
    public void onlyASingleValueIsJson() {
        assertThat(AiText.isValidJson("{\"a\":[1,2]}")).isTrue();
        assertThat(AiText.isValidJson("1\n2")).isFalse();
        assertThat(AiText.isValidJson("{\"a\":1} {\"b\":2}")).isFalse();
        assertThat(AiText.isValidJson("")).isFalse();
        // several values are not JSON, so their whitespace is kept
        assertThat(AiText.singleLineBody("1\n2", true)).isEqualTo("1 2");
    }

    @Test
    public void controlCharactersAreEscapedAndBinaryBodiesReplaced() {
        assertThat(AiText.singleLine("a\u001b[31mred\tb")).isEqualTo("a\\u001b[31mred\tb");
        assertThat(AiText.escapeControlCharacters("a\n\u0007", true)).isEqualTo("a\n\\u0007");
        assertThat(AiText.singleLineBody("PNG\u0000\u0001", false)).isEqualTo("[binary, 5 chars]");
        assertThat(AiText.singleLineBody("x".repeat(20) + "\u0001", false)).isEqualTo("x".repeat(20) + "\\u0001");
    }
}
