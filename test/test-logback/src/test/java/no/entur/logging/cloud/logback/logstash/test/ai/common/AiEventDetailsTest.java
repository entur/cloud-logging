package no.entur.logging.cloud.logback.logstash.test.ai.common;

import static com.google.common.truth.Truth.assertThat;
import static net.logstash.logback.marker.Markers.appendRaw;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import net.logstash.logback.marker.LogstashMarker;
import no.entur.logging.cloud.logback.logstash.test.ai.AiBodyMarker;

public class AiEventDetailsTest {

    private static AiEventDetails details(Marker marker) {
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "RESP", null, null);
        event.addMarker(marker);
        event.setMDCPropertyMap(Map.of());
        return AiEventDetails.of(event);
    }

    @Test
    public void placeholders() {
        assertThat(AiEventDetails.placeholders("a {} b \\{} c \\\\{}")).isEqualTo(2);
    }

    @Test
    public void bodyIsOnlyParsedAgainIfItDiffersFromTheOneInTheJsonOutput() {
        // the same body: valid JSON, as the event's JSON was parsed with it
        LogstashMarker same = appendRaw("http", "{\"headers\":{},\"body\":{\"a\" : 1}}");
        same.add(new AiBodyMarker("{\"a\" : 1}\n", true));
        assertThat(details(same).isValidJsonBody()).isTrue();

        // another body (for example the JSON output's is cut): parsed again
        LogstashMarker other = appendRaw("http", "{\"headers\":{},\"body\":{\"a\":1}}");
        other.add(new AiBodyMarker("{\"a\": tru\n e}", true));
        assertThat(details(other).isValidJsonBody()).isFalse();

        // without the AI body marker, the body is the one from the JSON output
        assertThat(details(appendRaw("http", "{\"headers\":{},\"body\":[1, 2]}")).isValidJsonBody()).isTrue();
    }
}
