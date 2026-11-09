package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class AiBodyMarkerTest {

    @Test
    public void bodyIsDecodedOnceWhenFirstNeeded() {
        AtomicInteger calls = new AtomicInteger();
        AiBodyMarker marker = new AiBodyMarker(() -> {
            calls.incrementAndGet();
            return "{\"a\":1}";
        }, true);

        assertThat(calls.get()).isEqualTo(0);
        assertThat(marker.getBody()).isEqualTo("{\"a\":1}");
        assertThat(marker.getBody()).isEqualTo("{\"a\":1}");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(marker.isJson()).isTrue();
    }

    @Test
    public void bodyWhichCannotBeDecodedIsLeftOut() {
        AiBodyMarker marker = new AiBodyMarker(() -> {
            throw new IllegalStateException("recycled");
        }, false);

        assertThat(marker.getBody()).isNull();
    }
}
