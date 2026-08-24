package no.entur.logging.cloud.azure.logback.logstash;

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

public class AzureOpenTelemetryTraceMdcJsonProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void writeTo_openTelemetryTraceFields_mappedToAzureFields() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put(AzureOpenTelemetryTraceMdcJsonProvider.OPENTELEMETRY_TRACE_ID_KEY, "06796866738c859f2f19b7cfb3214824");
        mdc.put(AzureOpenTelemetryTraceMdcJsonProvider.OPENTELEMETRY_SPAN_ID_KEY, "000000000000004a");

        JsonNode root = write(mdc);

        assertThat(root.get(AzureOpenTelemetryTraceMdcJsonProvider.AZURE_TRACE_KEY).asText())
                .isEqualTo("06796866738c859f2f19b7cfb3214824");
        assertThat(root.get(AzureOpenTelemetryTraceMdcJsonProvider.AZURE_SPAN_ID_KEY).asText())
                .isEqualTo("000000000000004a");
        assertThat(root.has(AzureOpenTelemetryTraceMdcJsonProvider.OPENTELEMETRY_TRACE_ID_KEY)).isFalse();
        assertThat(root.has(AzureOpenTelemetryTraceMdcJsonProvider.OPENTELEMETRY_SPAN_ID_KEY)).isFalse();
    }

    @Test
    void writeTo_unrelatedMdcFields_preserved() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put(AzureOpenTelemetryTraceMdcJsonProvider.OPENTELEMETRY_TRACE_ID_KEY, "abc");
        mdc.put("correlationId", "xyz123");
        mdc.put("userId", "user42");

        JsonNode root = write(mdc);

        assertThat(root.get("correlationId").asText()).isEqualTo("xyz123");
        assertThat(root.get("userId").asText()).isEqualTo("user42");
    }

    @Test
    void writeTo_collisionBetweenOtelAndAzureKey_otelKeyMappedFirst() throws Exception {
        // When MDC contains both the OTel key (trace_id) and the Azure target key (traceId),
        // the OTel key is translated to traceId and appears in the output.
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put(AzureOpenTelemetryTraceMdcJsonProvider.OPENTELEMETRY_TRACE_ID_KEY, "otel-trace-value");
        mdc.put(AzureOpenTelemetryTraceMdcJsonProvider.AZURE_TRACE_KEY, "existing-azure-trace");

        JsonNode root = write(mdc);

        // The OTel trace_id is mapped to traceId; readTree retains the last value on duplicate keys.
        assertThat(root.get(AzureOpenTelemetryTraceMdcJsonProvider.AZURE_TRACE_KEY)).isNotNull();
        assertThat(root.has(AzureOpenTelemetryTraceMdcJsonProvider.OPENTELEMETRY_TRACE_ID_KEY)).isFalse();
    }

    @Test
    void writeTo_emptyMdc_writesNothing() throws Exception {
        JsonNode root = write(new LinkedHashMap<>());
        assertThat(root.size()).isEqualTo(0);
    }

    @Test
    void writeTo_nullMdc_writesNothing() throws Exception {
        AzureOpenTelemetryTraceMdcJsonProvider provider = new AzureOpenTelemetryTraceMdcJsonProvider();
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getMDCPropertyMap()).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(stringWriter)) {
            generator.writeStartObject();
            provider.writeTo(generator, event);
            generator.writeEndObject();
        }
        JsonNode root = MAPPER.readTree(stringWriter.toString());
        assertThat(root.size()).isEqualTo(0);
    }

    private static JsonNode write(Map<String, String> mdcMap) throws Exception {
        return MAPPER.readTree(writeRaw(mdcMap));
    }

    private static String writeRaw(Map<String, String> mdcMap) throws Exception {
        AzureOpenTelemetryTraceMdcJsonProvider provider = new AzureOpenTelemetryTraceMdcJsonProvider();
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        Mockito.when(event.getMDCPropertyMap()).thenReturn(mdcMap);

        StringWriter stringWriter = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(stringWriter)) {
            generator.writeStartObject();
            provider.writeTo(generator, event);
            generator.writeEndObject();
        }
        return stringWriter.toString();
    }
}
