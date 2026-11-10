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
        System.clearProperty(CompactSettings.AI_MESSAGE_MAX_PROPERTY);
        System.clearProperty(CompactSettings.AI_BODY_MAX_PROPERTY);
        System.clearProperty(CompactSettings.AI_EXCLUDED_HEADERS_PROPERTY);
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
        System.setProperty(CompactSettings.AI_BODY_MAX_PROPERTY, "3");
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
        System.setProperty(CompactSettings.AI_EXCLUDED_HEADERS_PROPERTY, "X-Secret, other");
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
        String alias = AiAliases.aliasOf(uuid);

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
    public void truncationNoteIsLeftOutIfTheAiBodyIsComplete() {
        // the sink's note is about the body in the JSON output; the AI body marker has the whole body
        LogstashMarker complete = appendRaw("http", "{\"headers\":{},\"body\":\"ab\"}");
        complete.add(new AiBodyMarker("abcdef", false));
        assertThat(full(event("200 http://localhost/x (in 5 ms) [truncated ~4KB]", complete, null))).isEqualTo("200 http://localhost/x (in 5 ms) | body: abcdef");

        LoggingEvent jsonBody = event("POST /x [truncated ~12 bytes]", appendRaw("http", "{\"headers\":{},\"body\":\"ab\"}"), null);
        assertThat(full(jsonBody)).isEqualTo("POST /x [truncated ~12 bytes] | body: ab");
    }

    @Test
    public void longMessagesAreCutInTheCompactFileWithAnId() {
        System.setProperty(CompactSettings.AI_MESSAGE_MAX_PROPERTY, "6");
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "payload: 1234567890", null, null);
        event.setMDCPropertyMap(Map.of());
        EventIdConverter eventId = new EventIdConverter();
        eventId.start();

        assertThat(compact(event)).isEqualTo("payloa...[truncated, 19 chars]");
        assertThat(full(event)).isEqualTo("payload: 1234567890");
        assertThat(eventId.convert(event)).matches(" #[0-9a-f]{12}");

        LoggingEvent shortMessage = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "short", null, null);
        shortMessage.setMDCPropertyMap(Map.of());
        assertThat(eventId.convert(shortMessage)).isEmpty();
    }

    @Test
    public void bodyIsOnlyParsedAgainIfItDiffersFromTheOneInTheJsonOutput() {
        // the same body: valid JSON, as the event's JSON was parsed with it
        LogstashMarker same = appendRaw("http", "{\"headers\":{},\"body\":{\"a\" : 1}}");
        same.add(new AiBodyMarker("{\"a\" : 1}\n", true));
        assertThat(full(event("RESP", same, null))).isEqualTo("RESP | body: {\"a\":1}");
        assertThat(AiEventDetails.of(event("RESP", same, null)).isValidJsonBody()).isTrue();

        // another body (for example the JSON output's is cut): parsed, and not valid JSON here, so its whitespace is kept
        LogstashMarker other = appendRaw("http", "{\"headers\":{},\"body\":{\"a\":1}}");
        other.add(new AiBodyMarker("{\"a\": tru\n e}", true));
        assertThat(full(event("RESP", other, null))).isEqualTo("RESP | body: {\"a\": tru e}");
        assertThat(AiEventDetails.of(event("RESP", other, null)).isValidJsonBody()).isFalse();
        // without the AI body marker, the body is the one from the JSON output
        assertThat(AiEventDetails.of(event("RESP", appendRaw("http", "{\"headers\":{},\"body\":[1, 2]}"), null)).isValidJsonBody()).isTrue();
    }

    @Test
    public void normalizedNames() {
        assertThat(CompactRequestResponse.normalizedName("X-Correlation-Id")).isEqualTo("correlationid");
        assertThat(CompactRequestResponse.normalizedName("correlation_id")).isEqualTo("correlationid");
        assertThat(CompactRequestResponse.normalizedName("logging.googleapis.com/trace")).isEqualTo("trace");
    }
}
