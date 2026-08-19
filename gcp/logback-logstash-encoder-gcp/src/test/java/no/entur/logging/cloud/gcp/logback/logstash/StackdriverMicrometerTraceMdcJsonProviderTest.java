package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class StackdriverMicrometerTraceMdcJsonProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void writeTo_openTelemetryTraceFields_mappedToGcpSpecialFields() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put(StackdriverMicrometerTraceMdcJsonProvider.MICROMETER_TRACE_ID_KEY, "06796866738c859f2f19b7cfb3214824");
        mdc.put(StackdriverMicrometerTraceMdcJsonProvider.MICROMETER_SPAN_ID_KEY, "000000000000004a");
        mdc.put("correlationId", "abc123");

        JsonNode root = write(mdc);

        assertThat(root.get("logging.googleapis.com/trace").asText())
                .isEqualTo("projects/myProject/traces/06796866738c859f2f19b7cfb3214824");
        assertThat(root.get("logging.googleapis.com/spanId").asText())
                .isEqualTo("000000000000004a");
        assertThat(root.get("correlationId").asText()).isEqualTo("abc123");
        assertThat(root.has("trace")).isFalse();
        assertThat(root.has("traceId")).isFalse();
        assertThat(root.has("spanId")).isFalse();
    }

    @Test
    void writeTo_existingGcpTraceFields_preservedWithoutOpenTelemetryValues() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("logging.googleapis.com/trace", "existing-trace");
        mdc.put("logging.googleapis.com/spanId", "existing-span");

        JsonNode root = write(mdc);

        assertThat(root.get("logging.googleapis.com/trace").asText()).isEqualTo("existing-trace");
        assertThat(root.get("logging.googleapis.com/spanId").asText()).isEqualTo("existing-span");
    }

    private static JsonNode write(Map<String, String> mdcMap) throws Exception {
        StackdriverMicrometerTraceMdcJsonProvider provider =
                new StackdriverMicrometerTraceMdcJsonProvider("myProject");
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getMDCPropertyMap()).thenReturn(mdcMap);

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
