package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class CompactRequestResponseTest {

    @AfterEach
    public void cleanUp() {
        MDC.clear();
        System.clearProperty(AiLoggingControl.AI_BODY_MAX_PROPERTY);
        System.clearProperty(AiLoggingControl.AI_EXCLUDED_HEADERS_PROPERTY);
    }

    @Test
    public void compactMessageLeavesOutExcludedHeadersAndHeadersInMdc() {
        MDC.put("correlationId", "abc-123");
        CompactRequestResponse message = new CompactRequestResponse("GET http://localhost/x\n", CompactRequestResponse.DEFAULT_EXCLUDED_HTTP_HEADERS)
                .header("User-Agent", "Java")
                .header("X-Correlation-Id", "abc-123")
                .header("Content-Type", "application/json")
                .body("{\"a\":1}");

        assertThat(message.getMessage()).isEqualTo("GET http://localhost/x | user-agent: Java; x-correlation-id: abc-123; content-type: application/json | body: {\"a\":1}");
        assertThat(message.getCompactMessage()).isEqualTo("GET http://localhost/x | content-type: application/json | body: {\"a\":1}");
        assertThat(message.getMarker().isTruncated()).isFalse();
    }

    @Test
    public void bodyIsTruncatedInCompactMessageOnly() {
        System.setProperty(AiLoggingControl.AI_BODY_MAX_PROPERTY, "3");
        CompactRequestResponse message = new CompactRequestResponse("RESP", CompactRequestResponse.DEFAULT_EXCLUDED_GRPC_HEADERS)
                .header("grpc-encoding", "identity")
                .body("abcdef");

        assertThat(message.getMessage()).isEqualTo("RESP | grpc-encoding: identity | body: abcdef");
        assertThat(message.getCompactMessage()).isEqualTo("RESP | body: abc...[truncated, 6 chars]");
        assertThat(message.getMarker().isTruncated()).isTrue();
    }

    @Test
    public void excludedHeadersCanBeOverridden() {
        System.setProperty(AiLoggingControl.AI_EXCLUDED_HEADERS_PROPERTY, "X-Secret, other");
        CompactRequestResponse message = new CompactRequestResponse("REQ", CompactRequestResponse.DEFAULT_EXCLUDED_GRPC_HEADERS)
                .header("grpc-encoding", "identity")
                .header("x-secret", "s");

        assertThat(message.getCompactMessage()).isEqualTo("REQ | grpc-encoding: identity");
    }

    @Test
    public void converterPrintsCompactMessageAndEventIdOnlyIfTruncated() {
        System.setProperty(AiLoggingControl.AI_BODY_MAX_PROPERTY, "3");
        CompactRequestResponse message = new CompactRequestResponse("REQ", CompactRequestResponse.DEFAULT_EXCLUDED_GRPC_HEADERS)
                .header("te", "trailers")
                .body("abcdef");
        LoggingEvent event = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, message.getMessage(), null, null);
        event.addMarker(message.getMarker());

        assertThat(new CompactMessageConverter().convert(event)).isEqualTo("REQ | body: abc...[truncated, 6 chars]");

        EventIdConverter compact = new EventIdConverter();
        compact.start();
        EventIdConverter always = new EventIdConverter();
        always.setOptionList(List.of("always"));
        always.start();
        assertThat(compact.convert(event)).matches(" #[0-9a-f]{8}");
        assertThat(always.convert(event)).isEqualTo(compact.convert(event));

        LoggingEvent plain = new LoggingEvent("f", new LoggerContext().getLogger("x"), Level.INFO, "line 1\n  line 2", null, null);
        assertThat(compact.convert(plain)).isEmpty();
        assertThat(always.convert(plain)).matches(" #[0-9a-f]{8}");
        assertThat(new CompactMessageConverter().convert(plain)).isEqualTo("line 1 line 2");
    }
}
