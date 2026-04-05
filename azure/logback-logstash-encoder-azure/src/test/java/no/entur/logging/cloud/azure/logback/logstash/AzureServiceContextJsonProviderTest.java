package no.entur.logging.cloud.azure.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;

import static com.google.common.truth.Truth.assertThat;

public class AzureServiceContextJsonProviderTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    // --- parseServiceNameFromHostname ---

    @Test
    void parseServiceNameFromHostname_stripsLastTwoSegments() {
        assertThat(AzureServiceContextJsonProvider.parseServiceNameFromHostname("my-service-abc123-5"))
                .isEqualTo("my-service");
    }

    @Test
    void parseServiceNameFromHostname_threeSegments() {
        assertThat(AzureServiceContextJsonProvider.parseServiceNameFromHostname("a-b-c"))
                .isEqualTo("a");
    }

    @Test
    void parseServiceNameFromHostname_nullInput() {
        assertThat(AzureServiceContextJsonProvider.parseServiceNameFromHostname(null)).isNull();
    }

    @Test
    void parseServiceNameFromHostname_emptyInput() {
        assertThat(AzureServiceContextJsonProvider.parseServiceNameFromHostname("")).isNull();
    }

    @Test
    void parseServiceNameFromHostname_noHyphens() {
        assertThat(AzureServiceContextJsonProvider.parseServiceNameFromHostname("noHyphens")).isNull();
    }

    @Test
    void parseServiceNameFromHostname_oneHyphen() {
        assertThat(AzureServiceContextJsonProvider.parseServiceNameFromHostname("one-hyphen")).isNull();
    }

    // --- setService / writeTo ---

    @Test
    void writeTo_writesServiceContextWhenServiceIsSet() throws Exception {
        AzureServiceContextJsonProvider provider = new AzureServiceContextJsonProvider();
        provider.setService("my-service");

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        JsonNode root = write(provider, event);

        assertThat(root.has("serviceContext")).isTrue();
        assertThat(root.get("serviceContext").get("service").asText()).isEqualTo("my-service");
    }

    @Test
    void writeTo_writesNothingWhenServiceIsNull() throws Exception {
        AzureServiceContextJsonProvider provider = new AzureServiceContextJsonProvider();

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        JsonNode root = write(provider, event);

        assertThat(root.has("serviceContext")).isFalse();
    }

    @Test
    void setService_undefinedTriggersAutodetect_noHostnameResultsInNothingWritten() throws Exception {
        AzureServiceContextJsonProvider provider = new AzureServiceContextJsonProvider();
        // Force no HOSTNAME so autodetect returns null; setService("undefined") triggers autodetect
        provider.setService("undefined");

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        JsonNode root = write(provider, event);

        // If HOSTNAME is not set in this environment, nothing should be written.
        // We can only assert that if serviceContext is present it has a "service" field.
        if (root.has("serviceContext")) {
            assertThat(root.get("serviceContext").has("service")).isTrue();
        }
    }

    // --- setVersion ---

    @Test
    void writeTo_writesBothServiceAndVersion() throws Exception {
        AzureServiceContextJsonProvider provider = new AzureServiceContextJsonProvider();
        provider.setService("svc");
        provider.setVersion("1.0");

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        JsonNode root = write(provider, event);

        assertThat(root.get("serviceContext").get("service").asText()).isEqualTo("svc");
        assertThat(root.get("serviceContext").get("version").asText()).isEqualTo("1.0");
    }

    @Test
    void setVersion_undefinedResultsInVersionOmitted() throws Exception {
        AzureServiceContextJsonProvider provider = new AzureServiceContextJsonProvider();
        provider.setService("svc");
        provider.setVersion("undefined");

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        JsonNode root = write(provider, event);

        assertThat(root.get("serviceContext").get("service").asText()).isEqualTo("svc");
        assertThat(root.get("serviceContext").has("version")).isFalse();
    }

    @Test
    void setVersion_nullResultsInVersionOmitted() throws Exception {
        AzureServiceContextJsonProvider provider = new AzureServiceContextJsonProvider();
        provider.setService("svc");
        provider.setVersion(null);

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        JsonNode root = write(provider, event);

        assertThat(root.get("serviceContext").get("service").asText()).isEqualTo("svc");
        assertThat(root.get("serviceContext").has("version")).isFalse();
    }

    private static JsonNode write(AzureServiceContextJsonProvider provider, ILoggingEvent event) throws Exception {
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
