package no.entur.logging.cloud.logback.logstash.test.ai.compact;

import static com.google.common.truth.Truth.assertThat;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/** The hand-written text scanning of the compact file: the same results as the regular expressions it replaced, in linear time. */
public class CompactTextScanningTest {

    private static final int LONG = 400_000;

    private static <T> T fast(Supplier<T> supplier) {
        long start = System.nanoTime();
        T result = supplier.get();
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(2_000L);
        return result;
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
}
