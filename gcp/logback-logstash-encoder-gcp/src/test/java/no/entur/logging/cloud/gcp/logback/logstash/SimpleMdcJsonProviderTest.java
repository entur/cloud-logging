package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class SimpleMdcJsonProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void writeTo_mdcEntries_writtenAsTopLevelStringFields() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("traceId", "abc123");
        mdc.put("spanId", "def456");

        JsonNode root = write(mdc);

        assertThat(root.get("traceId").asText()).isEqualTo("abc123");
        assertThat(root.get("spanId").asText()).isEqualTo("def456");
    }

    @Test
    void writeTo_nullKey_isSkipped() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put(null, "someValue");
        mdc.put("key", "value");

        JsonNode root = write(mdc);

        assertThat(root.get("key").asText()).isEqualTo("value");
        assertThat(root.size()).isEqualTo(1);
    }

    @Test
    void writeTo_nullValue_isSkipped() throws Exception {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("nullVal", null);
        mdc.put("key", "value");

        JsonNode root = write(mdc);

        assertThat(root.has("nullVal")).isFalse();
        assertThat(root.get("key").asText()).isEqualTo("value");
    }

    @Test
    void writeTo_emptyMdcMap_writesNoFields() throws Exception {
        JsonNode root = write(Collections.emptyMap());
        assertThat(root.size()).isEqualTo(0);
    }

    @Test
    void writeTo_nullMdcMap_writesNoFields() throws Exception {
        JsonNode root = write(null);
        assertThat(root.size()).isEqualTo(0);
    }

    private static JsonNode write(Map<String, String> mdcMap) throws Exception {
        SimpleMdcJsonProvider provider = new SimpleMdcJsonProvider();
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
