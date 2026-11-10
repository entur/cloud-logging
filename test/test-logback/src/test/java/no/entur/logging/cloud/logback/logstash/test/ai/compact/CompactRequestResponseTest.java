package no.entur.logging.cloud.logback.logstash.test.ai.compact;

import static com.google.common.truth.Truth.assertThat;
import static net.logstash.logback.marker.Markers.appendRaw;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiEventDetails;

public class CompactRequestResponseTest {

    private static LoggingEvent event(String message, String http) {
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, message, null, null);
        event.addMarker(appendRaw("http", http));
        event.setMDCPropertyMap(Map.of());
        return event;
    }

    @Test
    public void bodyCutToTheSameLengthIsStillCut() {
        // 4096 characters plus "...[truncated, 4122 chars]" (26 characters) is as long as the original body
        AiEventDetails details = AiEventDetails.of(event("RESP", "{\"headers\":{},\"body\":\"" + "x".repeat(4122) + "\"}"));

        assertThat(CompactRequestResponse.isBodyTruncatedInCompactFile(details)).isTrue();
    }

    @Test
    public void normalizedNames() {
        assertThat(CompactRequestResponse.normalizedName("X-Correlation-Id")).isEqualTo("correlationid");
        assertThat(CompactRequestResponse.normalizedName("correlation_id")).isEqualTo("correlationid");
        assertThat(CompactRequestResponse.normalizedName("logging.googleapis.com/trace")).isEqualTo("trace");
    }
}
