package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.mockito.Mockito.*;

public class AbstractLogLevelLogstashLogbackSinkTest {

    public class MockLogLevelLogstashLogbackSink extends AbstractLogLevelLogstashLogbackSink {

        public MockLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, JsonFactory jsonFactory, int maxSize, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier, MessageComposer server, MessageComposer client) {
            super(logConsumer, logLevelEnabled, jsonFactory, maxSize, remoteHttpMessageContextSupplier, server, client);
        }

        @Override
        protected Marker newRequestSingleFieldAppendingMarker(HttpRequest request, String body, boolean wellformed) {
            return mock(Marker.class);
        }

        @Override
        protected Marker newResponseSingleFieldAppendingMarker(HttpResponse response, long millis, String body, boolean wellformed) {
            return mock(Marker.class);
        }
    }

    @Test
    public void testMimetype() {
        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("text/xml"));
        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/xml"));

        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("text/xml;charset=UTF-8"));
        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/xml;charset=UTF-8"));

        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/ehf+xml"));
        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/vnd.difi.dpi.lenke+xml"));

        Assertions.assertFalse(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/json"));
    }

    @Test
    public void testDoesNotPrintUnknownMimetypes() {
        MockLogLevelLogstashLogbackSink spy = createSink();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getContentType()).thenReturn("application/unknown");
        spy.createRequestMarker(request);

        verify(spy).newRequestSingleFieldAppendingMarker(request, null, false);
    }

    @Test
    public void testPrintsRemoteJsonMimetype() throws IOException {
        MockLogLevelLogstashLogbackSink spy = createSink();

        String json = "{}";

        HttpRequest request = mock(HttpRequest.class);
        when(request.getContentType()).thenReturn("application/json");
        when(request.getBodyAsString()).thenReturn(json);
        when(request.getOrigin()).thenReturn(Origin.REMOTE);
        spy.createRequestMarker(request);

        verify(spy).newRequestSingleFieldAppendingMarker(eq(request), anyString(), eq(true));
    }

    @Test
    public void testPrintsRemoteInvalidJsonMimetype() throws IOException {
        MockLogLevelLogstashLogbackSink spy = createSink();

        String json = "{[}";

        HttpRequest request = mock(HttpRequest.class);
        when(request.getContentType()).thenReturn("application/json");
        when(request.getBodyAsString()).thenReturn(json);
        when(request.getOrigin()).thenReturn(Origin.REMOTE);
        spy.createRequestMarker(request);

        verify(spy).newRequestSingleFieldAppendingMarker(eq(request), anyString(), eq(false));
    }

    @Test
    public void testPrintsLocalJsonMimetype() throws IOException {
        MockLogLevelLogstashLogbackSink spy = createSink();

        String json = "{}";

        HttpRequest request = mock(HttpRequest.class);
        when(request.getContentType()).thenReturn("application/json");
        when(request.getBodyAsString()).thenReturn(json);
        when(request.getOrigin()).thenReturn(Origin.LOCAL);
        spy.createRequestMarker(request);

        verify(spy).newRequestSingleFieldAppendingMarker(eq(request), anyString(), eq(true));
    }

    @Test
    public void testPrintsTooLargeLocalJsonMimetype() throws IOException {
        MockLogLevelLogstashLogbackSink spy = createSink();

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for(int i = 0; i < 2 * 1024; i++) {
            builder.append(" ");
        }
        builder.append("}");

        String json = builder.toString();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getContentType()).thenReturn("application/json");
        when(request.getBodyAsString()).thenReturn(json);
        when(request.getOrigin()).thenReturn(Origin.LOCAL);
        spy.createRequestMarker(request);

        verify(spy).newRequestSingleFieldAppendingMarker(eq(request), anyString(), eq(true));
    }

    @Test
    public void testPrintsTooLargeRemoteJsonMimetype() throws IOException {
        MockLogLevelLogstashLogbackSink spy = createSink();

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for(int i = 0; i < 2 * 1024; i++) {
            builder.append(" ");
        }
        builder.append("}");

        String json = builder.toString();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getContentType()).thenReturn("application/json");
        when(request.getBodyAsString()).thenReturn(json);
        when(request.getOrigin()).thenReturn(Origin.REMOTE);
        spy.createRequestMarker(request);

        verify(spy).newRequestSingleFieldAppendingMarker(eq(request), eq("{\"Logger\":\"Max body size of 1024 reached, rest of the document has been filtered.\"}"), eq(true));
    }

    @Test
    public void testPrintsTooLargeRemoteInvalidJsonMimetype() throws IOException {
        MockLogLevelLogstashLogbackSink spy = createSink();

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for(int i = 0; i < 2 * 1024; i++) {
            builder.append("a");
        }
        builder.append("}");

        String json = builder.toString();

        HttpRequest request = mock(HttpRequest.class);
        when(request.getContentType()).thenReturn("application/json");
        when(request.getBodyAsString()).thenReturn(json);
        when(request.getOrigin()).thenReturn(Origin.REMOTE);
        spy.createRequestMarker(request);

        verify(spy).newRequestSingleFieldAppendingMarker(eq(request), eq(json.substring(0, 1024)), eq(false));
    }
    
    private MockLogLevelLogstashLogbackSink createSink() {
        BiConsumer<Marker, String> logConsumer = mock(BiConsumer.class);
        int maxSize = 1024;
        RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier = mock(RemoteHttpMessageContextSupplier.class);

        when(remoteHttpMessageContextSupplier.verifyJsonSyntax(any(HttpRequest.class))).thenReturn(true);

        MessageComposer server = mock(MessageComposer.class);
        MessageComposer client = mock(MessageComposer.class);
        BooleanSupplier logLevelEnabled = () -> true;
        MockLogLevelLogstashLogbackSink sink = new MockLogLevelLogstashLogbackSink(logConsumer, logLevelEnabled, new MappingJsonFactory(), maxSize, remoteHttpMessageContextSupplier, server, client);

        return spy(sink);
    }

}
