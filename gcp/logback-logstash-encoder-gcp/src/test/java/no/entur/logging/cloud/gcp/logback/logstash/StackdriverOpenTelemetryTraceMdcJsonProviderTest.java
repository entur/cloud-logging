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

public class StackdriverOpenTelemetryTraceMdcJsonProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void writeTo_traceIdAndSpanId_writtenAsGcpFields() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        mdc.put("span_id", "00f067aa0ba902b7");

        JsonNode root = write(mdc, null);

        assertThat(root.get("logging.googleapis.com/trace").asText())
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(root.get("logging.googleapis.com/spanId").asText())
                .isEqualTo("00f067aa0ba902b7");
    }

    @Test
    void writeTo_projectIdSet_traceValueIsFullResourcePath() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");

        JsonNode root = write(mdc, "ent-products-dev");

        assertThat(root.get("logging.googleapis.com/trace").asText())
                .isEqualTo("projects/ent-products-dev/traces/4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void writeTo_noProjectId_traceValueFallsBackToBareTraceId() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");

        JsonNode root = write(mdc, null);

        assertThat(root.get("logging.googleapis.com/trace").asText())
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void writeTo_noTraceOrSpanId_writesNoGcpFields() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("someOtherKey", "someValue");

        JsonNode root = write(mdc, "ent-products-dev");

        assertThat(root.has("logging.googleapis.com/trace")).isFalse();
        assertThat(root.has("logging.googleapis.com/spanId")).isFalse();
    }

    @Test
    void writeTo_legacyTraceKeyAlreadyPresent_fieldWrittenExactlyOnce() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        mdc.put("logging.googleapis.com/trace", "projects/legacy-p-id");

        String json = writeRaw(mdc, "ent-products-dev");

        assertThat(countOccurrences(json, "\"logging.googleapis.com/trace\"")).isEqualTo(1);
    }

    @Test
    void writeTo_legacySpanKeyAlreadyPresent_fieldWrittenExactlyOnce() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("span_id", "00f067aa0ba902b7");
        mdc.put("logging.googleapis.com/spanId", "legacy-span-id");

        String json = writeRaw(mdc, null);

        assertThat(countOccurrences(json, "\"logging.googleapis.com/spanId\"")).isEqualTo(1);
    }

    @Test
    void writeTo_camelCaseTraceIdAndSpanId_writtenAsGcpFields() throws Exception {
        // Micrometer Tracing's MDC convention, used when a service instruments OpenTelemetry
        // manually instead of via the OTel Java agent.
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        mdc.put("spanId", "00f067aa0ba902b7");

        JsonNode root = write(mdc, null);

        assertThat(root.get("logging.googleapis.com/trace").asText())
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(root.get("logging.googleapis.com/spanId").asText())
                .isEqualTo("00f067aa0ba902b7");
    }

    @Test
    void writeTo_bothTraceIdConventionsPresent_snakeCaseTakesPrecedence() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_id", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        mdc.put("traceId", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

        JsonNode root = write(mdc, null);

        assertThat(root.get("logging.googleapis.com/trace").asText())
                .isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    void writeTo_sampledTraceFlags_writesTraceSampledTrue() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        mdc.put("trace_flags", "01");

        JsonNode root = write(mdc, null);

        assertThat(root.get("logging.googleapis.com/trace_sampled").asBoolean()).isTrue();
    }

    @Test
    void writeTo_unsampledTraceFlags_writesTraceSampledFalse() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        mdc.put("trace_flags", "00");

        JsonNode root = write(mdc, null);

        assertThat(root.get("logging.googleapis.com/trace_sampled").asBoolean()).isFalse();
    }

    @Test
    void writeTo_unparseableTraceFlags_omitsTraceSampledField() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        mdc.put("trace_flags", "not-hex");

        JsonNode root = write(mdc, null);

        assertThat(root.has("logging.googleapis.com/trace_sampled")).isFalse();
    }

    @Test
    void writeTo_legacyTraceSampledKeyAlreadyPresent_fieldWrittenExactlyOnce() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("trace_flags", "01");
        mdc.put("logging.googleapis.com/trace_sampled", "legacy-value");

        String json = writeRaw(mdc, null);

        assertThat(countOccurrences(json, "\"logging.googleapis.com/trace_sampled\"")).isEqualTo(1);
    }

    private static JsonNode write(Map<String, String> mdcMap, String projectId) throws Exception {
        return MAPPER.readTree(writeRaw(mdcMap, projectId));
    }

    private static String writeRaw(Map<String, String> mdcMap, String projectId) throws Exception {
        StackdriverOpenTelemetryTraceMdcJsonProvider provider = new StackdriverOpenTelemetryTraceMdcJsonProvider(projectId);
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

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
