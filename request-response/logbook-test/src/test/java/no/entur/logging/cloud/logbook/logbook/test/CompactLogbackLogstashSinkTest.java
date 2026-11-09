package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logback.logstash.test.CompactMessageMarker;
import no.entur.logging.cloud.logbook.DefaultRemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import no.entur.logging.cloud.logbook.ResponseSingleFieldAppendingMarker;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;
import org.zalando.logbook.test.MockHttpRequest;
import org.zalando.logbook.test.MockHttpResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CompactLogbackLogstashSinkTest {

    private final List<Marker> markers = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    private final CompactLogbackLogstashSink sink = new CompactLogbackLogstashSink((marker, message) -> {
        markers.add(marker);
        messages.add(message);
    }, () -> true, JsonMapper.builder().build(), 64 * 1024, new DefaultRemoteHttpMessageContextSupplier(),
            new MessageComposer(false, false, false, true, false), new MessageComposer(false, false, false, true, false));

    @Test
    public void keepsRequestMarkerAndJsonBodyContent() throws IOException {
        MockHttpRequest request = MockHttpRequest.create().withOrigin(Origin.LOCAL).withMethod("POST").withPath("/api")
                .withContentType("application/json").withBodyAsString("{\n  \"id\" : 1,\n  \"id\" : 2.50,\n  \"text\" : \"a  b\"\n}");

        sink.write(null, request);

        assertThat(messages.get(0)).isEqualTo("POST /api | body: {\"id\":1,\"id\":2.50,\"text\":\"a  b\"}");
        assertThat(markers.get(0)).isInstanceOf(RequestSingleFieldAppendingMarker.class);
        assertThat(findCompactMarker(markers.get(0))).isNotNull();
    }

    @Test
    public void includesTextBodiesAndKeepsXmlTokensApart() throws IOException {
        MockHttpRequest request = MockHttpRequest.create().withOrigin(Origin.LOCAL).withMethod("GET").withPath("/api");

        sink.write(correlation(), request, MockHttpResponse.create().withStatus(400).withContentType("text/plain")
                .withBodyAsString("Bad request:\nmissing id"));
        sink.write(correlation(), request, MockHttpResponse.create().withStatus(200).withContentType("application/xml")
                .withBodyAsString("<item\n  id=\"1\"/>"));
        sink.write(correlation(), request, MockHttpResponse.create().withStatus(200).withContentType("image/png")
                .withBodyAsString("binary"));

        assertThat(messages.get(0)).endsWith(" | body: Bad request: missing id");
        assertThat(messages.get(1)).endsWith(" | body: <item id=\"1\"/>");
        assertThat(messages.get(2)).doesNotContain("body");
        assertThat(markers.get(0)).isInstanceOf(ResponseSingleFieldAppendingMarker.class);
    }

    @Test
    public void logsWithoutBodyIfBodyCannotBeRead() throws IOException {
        HttpRequest request = spy(MockHttpRequest.create().withOrigin(Origin.LOCAL).withMethod("PUT").withPath("/api")
                .withContentType("application/json").withBodyAsString("{}"));
        doThrow(new IOException("closed")).when(request).getBodyAsString();

        sink.write(null, request);

        assertThat(messages.get(0)).isEqualTo("PUT /api");
    }

    private static Correlation correlation() {
        Correlation correlation = mock(Correlation.class);
        when(correlation.getDuration()).thenReturn(Duration.ofMillis(5));
        return correlation;
    }

    private static CompactMessageMarker findCompactMarker(Marker marker) {
        if (marker instanceof CompactMessageMarker compact) {
            return compact;
        }
        Iterator<Marker> iterator = marker.iterator();
        while (iterator.hasNext()) {
            CompactMessageMarker found = findCompactMarker(iterator.next());
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
