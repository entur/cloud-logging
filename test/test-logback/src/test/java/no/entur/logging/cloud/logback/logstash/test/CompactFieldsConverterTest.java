package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;
import static net.logstash.logback.marker.Markers.append;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class CompactFieldsConverterTest {

    @AfterEach
    public void cleanUp() {
        System.clearProperty(CompactSettings.AI_BODY_MAX_PROPERTY);
    }

    @Test
    public void fieldsAreCutAndTheEventGetsAnId() {
        System.setProperty(CompactSettings.AI_BODY_MAX_PROPERTY, "10");
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "Order rejected", null, null);
        event.addMarker(append("system", "abcdefghijklmnop"));

        CompactFieldsConverter converter = new CompactFieldsConverter();
        converter.start();
        assertThat(converter.convert(event)).isEqualTo(" | fields: {\"system\":...[truncated, 29 chars]");

        CompactEventIdConverter eventId = new CompactEventIdConverter();
        eventId.start();
        assertThat(eventId.convert(event)).matches(" #[0-9a-f]{12}");
    }
}
