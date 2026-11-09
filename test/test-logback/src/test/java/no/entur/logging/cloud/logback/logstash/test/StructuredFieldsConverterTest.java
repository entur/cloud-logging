package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;
import static net.logstash.logback.marker.Markers.append;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import no.entur.logging.cloud.api.DevOpsMarker;

public class StructuredFieldsConverterTest {

    @AfterEach
    public void cleanUp() {
        System.clearProperty(AiLoggingControl.AI_BODY_MAX_PROPERTY);
    }

    @Test
    public void printsMarkerFieldsAndKeyValuePairs() {
        LoggingEvent event = event("Order rejected");
        event.addMarker(append("system", Map.of("id", 1)));
        event.addKeyValuePair(new KeyValuePair("reason", "late"));

        assertThat(converter(null).convert(event)).isEqualTo(" | fields: {\"system\":{\"id\":1},\"reason\":\"late\"}");
    }

    @Test
    public void printsReferencedMarkers() {
        LoggingEvent event = event("Order rejected");
        event.addMarker(DevOpsMarker.errorTellMeTomorrow(append("system", "a")));

        assertThat(converter(null).convert(event)).isEqualTo(" | fields: {\"system\":\"a\"}");
    }

    @Test
    public void printsNothingWithoutFieldsOrForRequestResponse() {
        LoggingEvent plain = event("plain");
        assertThat(converter(null).convert(plain)).isEmpty();

        LoggingEvent devOps = event("devops");
        devOps.addMarker(DevOpsMarker.errorTellMeTomorrow());
        assertThat(converter(null).convert(devOps)).isEmpty();

        LoggingEvent requestResponse = event("GET /x | body: {}");
        requestResponse.addMarker(new CompactMessageMarker("GET /x", false));
        requestResponse.addKeyValuePair(new KeyValuePair("a", "b"));
        assertThat(converter(null).convert(requestResponse)).isEmpty();
    }

    @Test
    public void compactFileTruncatesFieldsAndShowsEventId() {
        System.setProperty(AiLoggingControl.AI_BODY_MAX_PROPERTY, "10");
        LoggingEvent event = event("Order rejected");
        event.addMarker(append("system", "abcdefghijklmnop"));

        assertThat(converter("compact").convert(event)).isEqualTo(" | fields: {\"system\":...[truncated, 29 chars]");
        assertThat(converter(null).convert(event)).isEqualTo(" | fields: {\"system\":\"abcdefghijklmnop\"}");

        EventIdConverter eventId = new EventIdConverter();
        eventId.start();
        assertThat(eventId.convert(event)).matches(" #[0-9a-f]{8}");
    }

    private static LoggingEvent event(String message) {
        return new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, message, null, null);
    }

    private static StructuredFieldsConverter converter(String option) {
        StructuredFieldsConverter converter = new StructuredFieldsConverter();
        if (option != null) {
            converter.setOptionList(List.of(option));
        }
        converter.start();
        return converter;
    }
}
