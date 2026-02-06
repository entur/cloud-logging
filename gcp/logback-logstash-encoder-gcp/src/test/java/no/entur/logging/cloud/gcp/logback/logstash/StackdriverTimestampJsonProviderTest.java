package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;

import static com.google.common.truth.Truth.assertThat;

public class StackdriverTimestampJsonProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void writesSecondsAndNanosWhenNanosecondsAreAvailable() throws Exception {
        StackdriverTimestampJsonProvider provider = new StackdriverTimestampJsonProvider();

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getTimeStamp()).thenReturn(1_712_345_678_901L);
        Mockito.when(event.getNanoseconds()).thenReturn(987_654_321);

        JsonNode root = write(provider, event);

        assertThat(root.has("timestamp")).isTrue();
        assertThat(root.get("timestamp").get("seconds").asLong()).isEqualTo(1_712_345_678L);
        assertThat(root.get("timestamp").get("nanos").asInt()).isEqualTo(987_654_321);
    }

    @Test
    void derivesNanosFromMillisecondsWhenNanosecondsAreUnavailable() throws Exception {
        StackdriverTimestampJsonProvider provider = new StackdriverTimestampJsonProvider();

        // 123 ms part should become 123_000_000 nanos
        long timestampMs = 1_712_345_678_123L;

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getTimeStamp()).thenReturn(timestampMs);
        Mockito.when(event.getNanoseconds()).thenReturn(-1);

        JsonNode root = write(provider, event);

        assertThat(root.has("timestamp")).isTrue();
        assertThat(root.get("timestamp").get("seconds").asLong()).isEqualTo(1_712_345_678L);
        assertThat(root.get("timestamp").get("nanos").asInt()).isEqualTo(123_000_000);
    }

    @Test
    void modsNanosecondsToWithinSecond() throws Exception {
        StackdriverTimestampJsonProvider provider = new StackdriverTimestampJsonProvider();

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getTimeStamp()).thenReturn(1_712_345_678_000L);
        Mockito.when(event.getNanoseconds()).thenReturn(1_234_567_890); // > 1s, but within int range

        JsonNode root = write(provider, event);

        assertThat(root.get("timestamp").get("nanos").asInt()).isEqualTo(234_567_890);
    }

    private static JsonNode write(StackdriverTimestampJsonProvider provider, ILoggingEvent event) throws Exception {
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
