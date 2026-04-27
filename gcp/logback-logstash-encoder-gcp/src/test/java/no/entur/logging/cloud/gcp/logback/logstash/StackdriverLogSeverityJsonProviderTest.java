package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;

import static com.google.common.truth.Truth.assertThat;

public class StackdriverLogSeverityJsonProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void getSeverity_trace_mapsToDebug() {
        ILoggingEvent event = mockEventWithLevel(Level.TRACE);
        assertThat(StackdriverLogSeverityJsonProvider.getSeverity(event)).isEqualTo(StackdriverSeverity.DEBUG);
    }

    @Test
    void getSeverity_debug_mapsToDebug() {
        ILoggingEvent event = mockEventWithLevel(Level.DEBUG);
        assertThat(StackdriverLogSeverityJsonProvider.getSeverity(event)).isEqualTo(StackdriverSeverity.DEBUG);
    }

    @Test
    void getSeverity_info_mapsToInfo() {
        ILoggingEvent event = mockEventWithLevel(Level.INFO);
        assertThat(StackdriverLogSeverityJsonProvider.getSeverity(event)).isEqualTo(StackdriverSeverity.INFO);
    }

    @Test
    void getSeverity_warn_mapsToWarning() {
        ILoggingEvent event = mockEventWithLevel(Level.WARN);
        assertThat(StackdriverLogSeverityJsonProvider.getSeverity(event)).isEqualTo(StackdriverSeverity.WARNING);
    }

    @Test
    void getSeverity_errorWithoutMarker_mapsToError() {
        ILoggingEvent event = mockEventWithLevel(Level.ERROR);
        Mockito.when(event.getMarker()).thenReturn(null);
        assertThat(StackdriverLogSeverityJsonProvider.getSeverity(event)).isEqualTo(StackdriverSeverity.ERROR);
    }

    @Test
    void getSeverity_errorWithTellMeTomorrowMarker_mapsToError() {
        ILoggingEvent event = mockEventWithLevel(Level.ERROR);
        Mockito.when(event.getMarker()).thenReturn(DevOpsMarker.errorTellMeTomorrow());
        assertThat(StackdriverLogSeverityJsonProvider.getSeverity(event)).isEqualTo(StackdriverSeverity.ERROR);
    }

    @Test
    void getSeverity_errorWithInterruptMyDinnerMarker_mapsToCritical() {
        ILoggingEvent event = mockEventWithLevel(Level.ERROR);
        Mockito.when(event.getMarker()).thenReturn(DevOpsMarker.errorInterruptMyDinner());
        assertThat(StackdriverLogSeverityJsonProvider.getSeverity(event)).isEqualTo(StackdriverSeverity.CRITICAL);
    }

    @Test
    void getSeverity_errorWithWakeMeUpRightNowMarker_mapsToAlert() {
        ILoggingEvent event = mockEventWithLevel(Level.ERROR);
        Mockito.when(event.getMarker()).thenReturn(DevOpsMarker.errorWakeMeUpRightNow());
        assertThat(StackdriverLogSeverityJsonProvider.getSeverity(event)).isEqualTo(StackdriverSeverity.ALERT);
    }

    @Test
    void writeTo_infoEvent_writesSeverityFieldWithCorrectValue() throws Exception {
        StackdriverLogSeverityJsonProvider provider = new StackdriverLogSeverityJsonProvider();
        ILoggingEvent event = mockEventWithLevel(Level.INFO);

        JsonNode root = write(provider, event);

        assertThat(root.has("severity")).isTrue();
        assertThat(root.get("severity").asText()).isEqualTo("INFO");
    }

    @Test
    void writeTo_usesFieldNameSeverityNotLevel() throws Exception {
        StackdriverLogSeverityJsonProvider provider = new StackdriverLogSeverityJsonProvider();
        ILoggingEvent event = mockEventWithLevel(Level.WARN);

        JsonNode root = write(provider, event);

        assertThat(root.has("severity")).isTrue();
        assertThat(root.has("level")).isFalse();
    }

    private static ILoggingEvent mockEventWithLevel(Level level) {
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getLevel()).thenReturn(level);
        return event;
    }

    private static JsonNode write(StackdriverLogSeverityJsonProvider provider, ILoggingEvent event) throws Exception {
        StringWriter stringWriter = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(stringWriter)) {
            generator.writeStartObject();
            provider.writeTo(generator, event);
            generator.writeEndObject();
        }
        return MAPPER.readTree(stringWriter.toString());
    }
}
