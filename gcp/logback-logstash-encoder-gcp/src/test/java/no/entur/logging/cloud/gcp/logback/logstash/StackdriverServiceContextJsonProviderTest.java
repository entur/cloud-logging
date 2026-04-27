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

public class StackdriverServiceContextJsonProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    // --- parseServiceNameFromHostname ---

    @Test
    void parseServiceNameFromHostname_typicalKubernetesHost_stripsLastTwoSegments() {
        assertThat(StackdriverServiceContextJsonProvider.parseServiceNameFromHostname("my-service-abc123-5"))
                .isEqualTo("my-service");
    }

    @Test
    void parseServiceNameFromHostname_threeSegments_returnsFirstSegment() {
        assertThat(StackdriverServiceContextJsonProvider.parseServiceNameFromHostname("a-b-c"))
                .isEqualTo("a");
    }

    @Test
    void parseServiceNameFromHostname_null_returnsNull() {
        assertThat(StackdriverServiceContextJsonProvider.parseServiceNameFromHostname(null)).isNull();
    }

    @Test
    void parseServiceNameFromHostname_emptyString_returnsNull() {
        assertThat(StackdriverServiceContextJsonProvider.parseServiceNameFromHostname("")).isNull();
    }

    @Test
    void parseServiceNameFromHostname_noHyphens_returnsNull() {
        assertThat(StackdriverServiceContextJsonProvider.parseServiceNameFromHostname("noHyphens")).isNull();
    }

    @Test
    void parseServiceNameFromHostname_oneHyphen_returnsNull() {
        assertThat(StackdriverServiceContextJsonProvider.parseServiceNameFromHostname("one-hyphen")).isNull();
    }

    // --- writeTo ---

    @Test
    void writeTo_withServiceSet_writesServiceContextObject() throws Exception {
        StackdriverServiceContextJsonProvider provider = new StackdriverServiceContextJsonProvider();
        provider.setService("my-app");

        JsonNode root = write(provider);

        assertThat(root.has("serviceContext")).isTrue();
        assertThat(root.get("serviceContext").get("service").asText()).isEqualTo("my-app");
    }

    @Test
    void writeTo_withNullService_writesNothing() throws Exception {
        StackdriverServiceContextJsonProvider provider = new StackdriverServiceContextJsonProvider();
        // service is null by default; no env var set in test environment

        JsonNode root = write(provider);

        assertThat(root.has("serviceContext")).isFalse();
        assertThat(root.size()).isEqualTo(0);
    }

    @Test
    void writeTo_withServiceAndVersion_writesBothFields() throws Exception {
        StackdriverServiceContextJsonProvider provider = new StackdriverServiceContextJsonProvider();
        provider.setService("my-app");
        provider.setVersion("1.2.3");

        JsonNode root = write(provider);

        assertThat(root.get("serviceContext").get("service").asText()).isEqualTo("my-app");
        assertThat(root.get("serviceContext").get("version").asText()).isEqualTo("1.2.3");
    }

    @Test
    void writeTo_withServiceAndNullVersion_omitsVersionField() throws Exception {
        StackdriverServiceContextJsonProvider provider = new StackdriverServiceContextJsonProvider();
        provider.setService("my-app");
        provider.setVersion(null);

        JsonNode root = write(provider);

        assertThat(root.get("serviceContext").has("version")).isFalse();
    }

    private static JsonNode write(StackdriverServiceContextJsonProvider provider) throws Exception {
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
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
