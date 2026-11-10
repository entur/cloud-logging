package no.entur.logging.cloud.logbook.logbook.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.AiBodyMarker;
import no.entur.logging.cloud.logback.logstash.test.FullMessageConverter;
import no.entur.logging.cloud.logback.logstash.test.FullFieldsConverter;
import no.entur.logging.cloud.logbook.DefaultRemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import no.entur.logging.cloud.logbook.ResponseSingleFieldAppendingMarker;
import no.entur.logging.cloud.logbook.ondemand.RequestOndemandSingleFieldAppendingMarker;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;
import org.zalando.logbook.Sink;
import org.zalando.logbook.test.MockHttpRequest;
import org.zalando.logbook.test.MockHttpResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class AiLogLevelLogstashLogbackSinkTest {

    private static final MessageComposer COMPOSER = new MessageComposer(false, false, false, true, false);

    private final List<Marker> markers = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    private final AiLogLevelLogstashLogbackSink sink = new AiLogLevelLogstashLogbackSink((marker, message) -> {
        markers.add(marker);
        messages.add(message);
    }, () -> true, JsonMapper.builder().build(), 64 * 1024, new DefaultRemoteHttpMessageContextSupplier(), COMPOSER, COMPOSER);

    @Test
    public void messageIsAsWithoutAiModeAndTheBodyIsAttached() throws IOException {
        String body = "{\n  \"id\" : 1,\n  \"id\" : 2.50\n}";
        MockHttpRequest request = MockHttpRequest.create().withOrigin(Origin.LOCAL).withMethod("POST").withPath("/api")
                .withContentType("application/json").withBodyAsString(body);

        List<String> standardMessages = new ArrayList<>();
        Sink standard = new LogLevelLogstashLogbackSink((marker, message) -> standardMessages.add(message), () -> true,
                JsonMapper.builder().build(), 64 * 1024, new DefaultRemoteHttpMessageContextSupplier(), COMPOSER, COMPOSER);
        standard.write(null, request);
        sink.write(null, request);

        assertThat(messages).isEqualTo(standardMessages);
        assertThat(messages.get(0)).isEqualTo("POST /api");
        assertThat(markers.get(0)).isInstanceOf(RequestSingleFieldAppendingMarker.class);
        AiBodyMarker bodyMarker = findBodyMarker(markers.get(0));
        assertThat(bodyMarker.getBody()).isEqualTo(body);
        assertThat(bodyMarker.isJson()).isTrue();
    }

    @Test
    public void textBodiesAreAttachedButNotBinaryOnes() throws IOException {
        MockHttpRequest request = MockHttpRequest.create().withOrigin(Origin.LOCAL).withMethod("GET").withPath("/api");

        sink.write(correlation(), request, MockHttpResponse.create().withStatus(400).withContentType("text/plain")
                .withBodyAsString("Bad request:\nmissing id"));
        sink.write(correlation(), request, MockHttpResponse.create().withStatus(200).withContentType("image/png")
                .withBodyAsString("binary"));

        assertThat(markers.get(0)).isInstanceOf(ResponseSingleFieldAppendingMarker.class);
        assertThat(findBodyMarker(markers.get(0)).getBody()).isEqualTo("Bad request:\nmissing id");
        assertThat(findBodyMarker(markers.get(1))).isNull();
    }

    @Test
    public void logsWithoutBodyIfBodyCannotBeRead() throws IOException {
        HttpRequest request = spy(MockHttpRequest.create().withOrigin(Origin.LOCAL).withMethod("PUT").withPath("/api")
                .withContentType("text/plain").withBodyAsString("x"));
        doThrow(new IOException("closed")).when(request).getBody();

        sink.write(null, request);

        assertThat(messages.get(0)).isEqualTo("PUT /api");
        assertThat(findBodyMarker(markers.get(0))).isNull();
    }

    @Test
    public void ondemandSinkAttachesTheBodyToItsOwnMarker() throws IOException {
        AiOndemandLogLevelLogstashLogbackSink ondemand = new AiOndemandLogLevelLogstashLogbackSink((marker, message) -> {
            markers.add(marker);
            messages.add(message);
        }, () -> true, JsonMapper.builder().build(), null, null, 64 * 1024, 64 * 1024, new DefaultRemoteHttpMessageContextSupplier(),
                COMPOSER, COMPOSER);

        ondemand.write(null, MockHttpRequest.create().withOrigin(Origin.LOCAL).withMethod("POST").withPath("/api")
                .withContentType("application/json").withBodyAsString("{\"a\":1}"));

        assertThat(markers.get(0)).isInstanceOf(RequestOndemandSingleFieldAppendingMarker.class);
        assertThat(findBodyMarker(markers.get(0)).getBody()).isEqualTo("{\"a\":1}");
    }

    @Test
    public void malformedJsonBodyOfALocalRequestKeepsHeadersAndBodyInTheAiFiles() throws IOException {
        // trusted as well-formed, so the request-response marker writes it as is and the event's JSON is not valid JSON
        sink.write(null, MockHttpRequest.create().withOrigin(Origin.LOCAL).withMethod("POST").withPath("/api")
                .withHeaders(HttpHeaders.of("x-test", "1")).withContentType("application/json").withBodyAsString("{\"a\":tru e}"));

        LoggerContext context = new LoggerContext();
        LoggingEvent event = new LoggingEvent("f", context.getLogger("x"), Level.INFO, messages.get(0), null, null);
        event.addMarker(markers.get(0));
        event.setMDCPropertyMap(Map.of());
        FullFieldsConverter fields = new FullFieldsConverter();
        fields.start();

        assertThat(new FullMessageConverter().convert(event)).isEqualTo("POST /api");
        assertThat(fields.convert(event)).contains("\"headers\":{\"x-test\":[\"1\"]}");
        assertThat(fields.convert(event)).contains("\"body\":{\"a\":tru e}");
    }

    private static Correlation correlation() {
        Correlation correlation = mock(Correlation.class);
        when(correlation.getDuration()).thenReturn(Duration.ofMillis(5));
        return correlation;
    }

    private static AiBodyMarker findBodyMarker(Marker marker) {
        if (marker instanceof AiBodyMarker bodyMarker) {
            return bodyMarker;
        }
        Iterator<Marker> iterator = marker.iterator();
        while (iterator.hasNext()) {
            AiBodyMarker found = findBodyMarker(iterator.next());
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
