package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;
import static net.logstash.logback.marker.Markers.appendRaw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import net.logstash.logback.marker.LogstashMarker;

public class CompactRequestResponseTest {

    @AfterEach
    public void cleanUp() {
        System.clearProperty(AiLoggingControl.AI_BODY_MAX_PROPERTY);
        System.clearProperty(AiLoggingControl.AI_EXCLUDED_HEADERS_PROPERTY);
    }

    private static LoggingEvent event(String message, Marker marker, Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, message, null, null);
        event.addMarker(marker);
        event.setMDCPropertyMap(mdc == null ? Map.of() : mdc);
        return event;
    }

    private static String compact(LoggingEvent event) {
        return new CompactMessageConverter().convert(event);
    }

    private static String full(LoggingEvent event) {
        return new AliasMessageConverter().convert(event);
    }

    @Test
    public void messageIsUnchangedAndHeadersAndBodyComeFromTheMarker() {
        Map<String, String> mdc = new HashMap<>();
        mdc.put("correlationId", "abc-123");
        mdc.put("attempt", "1");
        LoggingEvent event = event("GET http://localhost/x\n", appendRaw("http", """
                {"method":"GET","headers":{"user-agent":["Java"],"x-correlation-id":["abc-123"],"content-type":["application/json"],
                "x-retry-count":["1"]},"body":{
                  "a" : 1, "a" : 2.50
                }}"""), mdc);

        assertThat(event.getFormattedMessage()).isEqualTo("GET http://localhost/x\n");
        assertThat(full(event)).isEqualTo("GET http://localhost/x | user-agent: Java; x-correlation-id: abc-123; content-type: application/json; x-retry-count: 1 | body: {\"a\":1,\"a\":2.50}");
        // the correlation id header repeats the MDC; the retry count only has the same value as an unrelated MDC entry
        assertThat(compact(event)).isEqualTo("GET http://localhost/x | content-type: application/json; x-retry-count: 1 | body: {\"a\":1,\"a\":2.50}");
    }

    @Test
    public void bodyIsCutInCompactFileOnlyAndEventGetsAnId() {
        System.setProperty(AiLoggingControl.AI_BODY_MAX_PROPERTY, "3");
        LoggingEvent event = event("RESP", appendRaw("http", "{\"headers\":{},\"body\":\"abcdef\"}"), null);

        assertThat(full(event)).isEqualTo("RESP | body: abcdef");
        assertThat(compact(event)).isEqualTo("RESP | body: abc...[truncated, 6 chars]");

        EventIdConverter eventId = new EventIdConverter();
        eventId.start();
        assertThat(eventId.convert(event)).matches(" #[0-9a-f]{12}");
        assertThat(eventId.convert(event("RESP", appendRaw("http", "{\"headers\":{},\"body\":\"abc\"}"), null))).isEmpty();
    }

    @Test
    public void bodyCutToTheSameLengthIsStillCut() {
        // 4096 characters plus "...[truncated, 4122 chars]" (26 characters) is as long as the original body
        AiEventDetails details = AiEventDetails.of(event("RESP", appendRaw("http", "{\"headers\":{},\"body\":\"" + "x".repeat(4122) + "\"}"), null));

        assertThat(CompactRequestResponse.isBodyTruncatedInCompactFile(details)).isTrue();
    }

    @Test
    public void excludedHeadersCanBeOverridden() {
        System.setProperty(AiLoggingControl.AI_EXCLUDED_HEADERS_PROPERTY, "X-Secret, other");
        LoggingEvent event = event("REQ", appendRaw("http", "{\"headers\":{\"user-agent\":[\"Java\"],\"x-secret\":[\"s\"]}}"), null);

        assertThat(compact(event)).isEqualTo("REQ | user-agent: Java");
    }

    @Test
    public void grpcHeadersWithJsonValues() {
        LoggingEvent event = event("RESP INVALID_ARGUMENT /Greeting/greet", appendRaw("http", """
                {"headers":{"content-type":["application/grpc"],"grpc-status":["3"],"grpc-status-details-bin":[{"code":3,"details":[{"field":"name"}]}]}}"""), null);

        assertThat(compact(event)).isEqualTo("RESP INVALID_ARGUMENT /Greeting/greet | grpc-status: 3; grpc-status-details-bin: {\"code\":3,\"details\":[{\"field\":\"name\"}]}");
    }

    @Test
    public void uuidsAndHexIdsInHeadersAreAliasedAndAnnouncedInTheFullFile() {
        String uuid = UUID.randomUUID().toString();
        LoggingEvent event = event("GET /x", appendRaw("http",
                "{\"headers\":{\"x-request-id\":[\"" + uuid + "\"],\"traceparent\":[\"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01\"]}}"), null);
        String alias = MdcAliasConverter.aliasOf(uuid);

        assertThat(compact(event)).matches("GET /x \\| x-request-id: " + alias + "; traceparent: 00-h\\d+-h\\d+-01");
        assertThat(full(event)).startsWith("GET /x | x-request-id: " + alias + "(" + uuid + ")");
        assertThat(full(event)).startsWith("GET /x | x-request-id: " + alias + ";");
    }

    @Test
    public void bodyMarkerReplacesTheBodyOfTheRequestResponseMarker() {
        LogstashMarker marker = appendRaw("http", "{\"headers\":{},\"body\":\"ab\"}");
        marker.add(new AiBodyMarker("line 1\n  line 2", false));
        assertThat(full(event("RESP", marker, null))).isEqualTo("RESP | body: line 1 line 2");

        LogstashMarker binary = appendRaw("http", "{\"headers\":{}}");
        binary.add(new AiBodyMarker("a\u0000b", false));
        assertThat(full(event("RESP", binary, null))).isEqualTo("RESP | body: [binary, 3 chars]");
    }

    @Test
    public void nullMdcValuesAreAllowed() {
        Map<String, String> mdc = new HashMap<>();
        mdc.put("empty", null);
        LoggingEvent event = event("REQ", appendRaw("http", "{\"headers\":{\"empty\":[\"x\"]}}"), mdc);

        assertThat(compact(event)).isEqualTo("REQ | empty: x");
    }

    @Test
    public void httpFieldWithoutHeadersIsNotRequestResponse() {
        LoggingEvent event = event("m", appendRaw("http", "{\"status\":200}"), null);
        event.addKeyValuePair(new KeyValuePair("k", "v"));

        assertThat(compact(event)).isEqualTo("m");
        StructuredFieldsConverter fields = new StructuredFieldsConverter();
        fields.setOptionList(List.of("compact"));
        fields.start();
        assertThat(fields.convert(event)).isEqualTo(" | fields: {\"http\":{\"status\":200},\"k\":\"v\"}");
    }

    @Test
    public void otherRequestResponseDetailsAreInTheFullFileOnly() {
        LoggingEvent event = event("GET /x", appendRaw("http", "{\"origin\":\"local\",\"type\":\"request\",\"protocol\":\"HTTP/1.1\","
                + "\"remote\":\"127.0.0.1\",\"headers\":{\"a\":[\"b\"]},\"body\":\"x\",\"deadline-in\":{\"ms\":500}}"), null);

        assertThat(full(event)).isEqualTo("GET /x | a: b | body: x");
        assertThat(fields(event, false)).isEqualTo(" | fields: {\"http\":{\"origin\":\"local\",\"type\":\"request\",\"protocol\":\"HTTP/1.1\","
                + "\"remote\":\"127.0.0.1\",\"deadline-in\":{\"ms\":500}}}");
        assertThat(fields(event, true)).isEmpty();
    }

    @Test
    public void structuredDataWhichIsNotValidJsonIsKeptWhole() {
        // a malformed JSON body from a local request is written as is by the request-response marker
        LoggingEvent event = event("POST /x", appendRaw("http", "{\"headers\":{\"content-type\":[\"application/json\"]},\"body\":{\"a\":tru\n e}}"), null);

        assertThat(compact(event)).isEqualTo("POST /x");
        assertThat(fields(event, true)).isEqualTo(" | fields: {\"http\":{\"headers\":{\"content-type\":[\"application/json\"]},\"body\":{\"a\":tru e}}}");
        assertThat(fields(event, false)).isEqualTo(fields(event, true));
    }

    private static String fields(LoggingEvent event, boolean compact) {
        StructuredFieldsConverter converter = new StructuredFieldsConverter();
        converter.setOptionList(compact ? List.of("compact") : List.of());
        converter.start();
        return converter.convert(event);
    }

    @Test
    public void normalizedNames() {
        assertThat(CompactRequestResponse.normalizedName("X-Correlation-Id")).isEqualTo("correlationid");
        assertThat(CompactRequestResponse.normalizedName("correlation_id")).isEqualTo("correlationid");
        assertThat(CompactRequestResponse.normalizedName("logging.googleapis.com/trace")).isEqualTo("trace");
    }
}
