package no.entur.logging.cloud.logback.logstash.test.ai.common;

import static com.google.common.truth.Truth.assertThat;

import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** The hand-written UUID and hex id scanning gives the same results as the regular expressions it replaced. */
public class IdScanningTest {

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern UUID_OR_HEX_ID_TOKEN = Pattern.compile("(" + UUID_PATTERN.pattern() + ")"
            + "|(?<![0-9A-Za-z])(?=[0-9a-f]*[a-f])(?=[0-9a-f]*[0-9])[0-9a-f]{12,32}(?![0-9A-Za-z])");

    private static String byRegex(Pattern pattern, String text) {
        return pattern.matcher(text).replaceAll(m -> Matcher.quoteReplacement(m.groupCount() > 0 && m.group(1) == null ? "<h:" + m.group() + ">" : "<u:" + m.group() + ">"));
    }

    @Test
    public void sameResultsAsTheRegularExpressions() {
        Random random = new Random(1);
        String[] parts = {"0", "7", "a", "f", "A", "F", "g", "z", "Z", "-", " ", "/", "(", "_", "\n",
                UUID.randomUUID().toString(), UUID.randomUUID().toString().toUpperCase(), "00f067aa0ba902b7", "4bf92f3577b34da6a3ce929d0e0e4736",
                "0123456789abcdef0123456789abcdef0", "abcdefabcdef", "123456789012", "0a1b2c3d4e5f"};
        for (int i = 0; i < 20_000; i++) {
            StringBuilder text = new StringBuilder();
            int length = random.nextInt(30);
            for (int j = 0; j < length; j++) {
                text.append(parts[random.nextInt(parts.length)]);
            }
            String s = text.toString();
            assertThat(AiAliases.replaceUuids(s, uuid -> "<u:" + uuid + ">")).isEqualTo(byRegex(UUID_PATTERN, s));
            assertThat(AiAliases.replaceIds(s, uuid -> "<u:" + uuid + ">", hexId -> "<h:" + hexId + ">")).isEqualTo(byRegex(UUID_OR_HEX_ID_TOKEN, s));
        }
    }

    @Test
    public void examples() {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        assertThat(AiAliases.replaceUuids("id=" + uuid + "-x", v -> "U")).isEqualTo("id=U-x");
        assertThat(AiAliases.replaceUuids("no ids-here", v -> "U")).isEqualTo("no ids-here");
        assertThat(AiAliases.replaceIds("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", v -> "U", v -> "H")).isEqualTo("00-H-H-01");
        // the hex digits of a UUID are not hex ids
        assertThat(AiAliases.replaceIds(uuid, v -> "U", v -> "H")).isEqualTo("U");
    }
}
