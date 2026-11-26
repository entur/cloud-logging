package no.entur.logging.cloud.logback.logstash.test.ai.common;

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
    public void lineBreaksTakeLinearTime() {
        // quadratic before: 1.5 s for 40,000 spaces, so about 6 minutes for these 400,000
        String text = "x" + " ".repeat(400_000) + "y\n \t z";
        long start = System.nanoTime();
        assertThat(AiText.singleLine(text)).isEqualTo("x" + " ".repeat(400_000) + "y z");
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(2_000L);

        assertThat(AiText.singleLine("a \t\r\n \n  b\r\nc\n")).isEqualTo("a b c ");
    }

    @Test
    public void cutsNotInsideAnAliasOrSurrogatePair() {
        assertThat(AiText.cut("abc", 5)).isEqualTo("abc");
        assertThat(AiText.cut("id u1234 x", 5)).isEqualTo("id ");
        assertThat(AiText.cut("id u1234 x", 4)).isEqualTo("id ");
        assertThat(AiText.cut("id u1234 x", 8)).isEqualTo("id u1234");
        // not an alias: a digit run in a word, or a cut after the alias
        assertThat(AiText.cut("abu1234", 5)).isEqualTo("abu12");
        assertThat(AiText.cut("x 12345", 4)).isEqualTo("x 12");
        assertThat(AiText.cut("ab\uD83D\uDE00c", 3)).isEqualTo("ab");
    }

    @Test
    public void squeezesTableBordersAndPadding() {
        // WireMock's "Request was not matched": the differing header was beyond the part which was kept
        String table = "Request was not matched\n=======================\n\n" + "-".repeat(119) + "\n"
                + "| Closest stub" + " ".repeat(45) + "| Request" + " ".repeat(50) + "|\n" + "-".repeat(119) + "\n"
                + "X-Api-Uid: uid-30" + " ".repeat(41) + "| X-Api-Uid: key-30" + " ".repeat(29) + "<<<<< Header does not match\n";
        assertThat(AiText.squeeze(AiText.singleLine(table))).isEqualTo("Request was not matched === --- | Closest stub | Request | --- "
                + "X-Api-Uid: uid-30 | X-Api-Uid: key-30 <<< Header does not match ");
        // up to 3 of the same character, letters, digits (and so aliases) and single spaces are kept
        assertThat(AiText.squeeze("a...b -- c u1111 2000 x")).isSameInstanceAs("a...b -- c u1111 2000 x");
        assertThat(AiText.squeeze("a\t\tb\tc ****")).isEqualTo("a b c ***");
        assertThat(AiText.squeeze("\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00")).isEqualTo("\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00");
        // only if longer than the limit
        assertThat(AiText.squeezeIfLonger("a     b", 7)).isEqualTo("a     b");
        assertThat(AiText.squeezeIfLonger("a     b", 6)).isEqualTo("a b");
        assertThat(AiText.squeezeIfLonger("a     b", 0)).isEqualTo("a     b");
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
