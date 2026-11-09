package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * The hand-written text scanning which replaced regular expressions: the same results, and linear time on the inputs which made the
 * regular expressions take quadratic time (long runs of spaces, many repeated words).
 */
public class TextScanningTest {

    private static final int LONG = 400_000;

    private static <T> T fast(Supplier<T> supplier) {
        long start = System.nanoTime();
        T result = supplier.get();
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(2_000L);
        return result;
    }

    @Test
    public void lineBreaks() {
        assertThat(AiText.singleLine("a \t\r\n \n  b\r\nc\n")).isEqualTo("a b c ");
        assertThat(AiText.singleLine("a b\u0085c")).isEqualTo("a b c");
        assertThat(AiText.singleLine("a  \t b")).isEqualTo("a  \t b");
        assertThat(fast(() -> AiText.singleLine("x" + " ".repeat(LONG) + "y\nz"))).isEqualTo("x" + " ".repeat(LONG) + "y z");
        assertThat(fast(() -> AiText.singleLine("\n".repeat(LONG) + "x"))).isEqualTo(" x");
    }

    @Test
    public void truncationNote() {
        assertThat(CompactRequestResponse.withoutTruncatedNote("POST /x [truncated ~12 bytes]")).isEqualTo("POST /x");
        assertThat(CompactRequestResponse.withoutTruncatedNote("200 /x (in 5 ms)  [truncated ~4KB]")).isEqualTo("200 /x (in 5 ms)");
        assertThat(CompactRequestResponse.withoutTruncatedNote("GET /x [truncated ~KB]")).isEqualTo("GET /x [truncated ~KB]");
        assertThat(CompactRequestResponse.withoutTruncatedNote("GET /x [truncated ~4KB] more")).isEqualTo("GET /x [truncated ~4KB] more");
        assertThat(CompactRequestResponse.withoutTruncatedNote("GET /x [truncated ~4 KB]")).isEqualTo("GET /x [truncated ~4 KB]");
        String spaces = "GET /x" + " ".repeat(LONG) + "y]";
        assertThat(fast(() -> CompactRequestResponse.withoutTruncatedNote(spaces))).isSameInstanceAs(spaces);
    }

    @Test
    public void skippedNote() {
        assertThat(SingleLineRootCauseFirstThrowableProxyConverter.withoutSkippedNote("at a.B.m(B.java:1) [3 skipped]")).isEqualTo("at a.B.m(B.java:1)");
        assertThat(SingleLineRootCauseFirstThrowableProxyConverter.withoutSkippedNote("at a.B.m(B.java:1) [x skipped]")).isEqualTo("at a.B.m(B.java:1) [x skipped]");
        assertThat(SingleLineRootCauseFirstThrowableProxyConverter.withoutSkippedNote("at a.B.m(B.java:1) [ skipped]")).isEqualTo("at a.B.m(B.java:1) [ skipped]");
        assertThat(SingleLineRootCauseFirstThrowableProxyConverter.withoutSkippedNote("3 skipped]")).isEqualTo("3 skipped]");
        String spaces = "at a.B.m(B" + " ".repeat(LONG) + ".java:1)";
        assertThat(fast(() -> SingleLineRootCauseFirstThrowableProxyConverter.withoutSkippedNote(spaces))).isSameInstanceAs(spaces);
    }

    @Test
    public void serverStartedOnPort() {
        assertThat(QuietLoggersFilter.isServerStartedOnPort("Tomcat started on port 8080 (http)")).isTrue();
        assertThat(QuietLoggersFilter.isServerStartedOnPort("gRPC Server STARTED, listening on address: [/[::]:9090], Port: 9090")).isTrue();
        assertThat(QuietLoggersFilter.isServerStartedOnPort("Started on transport x")).isFalse();
        assertThat(QuietLoggersFilter.isServerStartedOnPort("port opened, server restarted")).isFalse();
        assertThat(fast(() -> QuietLoggersFilter.isServerStartedOnPort("started ".repeat(LONG / 8)))).isFalse();
    }

    @Test
    public void hexIds() {
        assertThat(MdcAliasConverter.isHexId("00f067aa0ba902b7")).isTrue();
        assertThat(MdcAliasConverter.isHexId("4bf92f3577b34da6a3ce929d0e0e4736")).isTrue();
        assertThat(MdcAliasConverter.isHexId("123456789012")).isFalse();
        assertThat(MdcAliasConverter.isHexId("abcdefabcdef")).isFalse();
        assertThat(MdcAliasConverter.isHexId("00F067AA0BA902B7")).isFalse();
        assertThat(MdcAliasConverter.isHexId("0a0a0a0a0a0")).isFalse();
        assertThat(MdcAliasConverter.isHexId("0a".repeat(17))).isFalse();
    }

    @Test
    public void eventIdDigits() {
        assertThat(EventIdConverter.hex12(0xabL)).isEqualTo("0000000000ab");
        assertThat(EventIdConverter.hex12(0xFFFF_FFFF_FFFFL)).isEqualTo("ffffffffffff");
        assertThat(EventIdConverter.hex12(0L)).isEqualTo("000000000000");
    }
}
