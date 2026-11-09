package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logback.logstash.test.CompactMessageMarker;
import no.entur.logging.cloud.logback.logstash.test.CompactRequestResponse;
import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import no.entur.logging.cloud.logbook.DefaultRemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.RequestResponseSingleFieldAppendingMarker;
import org.slf4j.Marker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Compact request-response logging message for tooling (AI mode): a single line without pretty-printing or colors,
 * in the form <code>message | header: value; header: value | body: &lt;body&gt;</code>. The compact file gets a shorter variant
 * (see {@link CompactRequestResponse}). The event keeps the regular request-response marker, so the JSON output (used by
 * logging assertions) has the same <code>http</code> fields as without AI mode.
 */
public class CompactLogbackLogstashSink extends LogLevelLogstashLogbackSink {

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public CompactLogbackLogstashSink build() {
            if (maxBodySize == -1) {
                throw new IllegalStateException("Expected max body size");
            }
            if (maxSize == -1) {
                throw new IllegalStateException("Expected max size");
            }
            if (logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if (level == null) {
                throw new IllegalStateException("Expected log level");
            }
            if (client == null) {
                throw new IllegalStateException("Expected client message composer");
            }
            if (server == null) {
                throw new IllegalStateException("Expected server message composer");
            }
            if (jsonMapper == null) {
                jsonMapper = JsonMapper.builder().build();
            }
            if (remoteHttpMessageContextSupplier == null) {
                remoteHttpMessageContextSupplier = new DefaultRemoteHttpMessageContextSupplier();
            }
            return new CompactLogbackLogstashSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), jsonMapper,
                    Math.min(maxBodySize, maxSize), remoteHttpMessageContextSupplier, server, client);
        }
    }

    public CompactLogbackLogstashSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled,
                                      JsonMapper jsonMapper, int maxSize, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier,
                                      MessageComposer server, MessageComposer client) {
        super(logConsumer, logLevelEnabled, jsonMapper, maxSize, remoteHttpMessageContextSupplier, server, client);
    }

    @Override
    public void write(Precorrelation precorrelation, HttpRequest request) throws IOException {
        StringBuilder messageBuilder = new StringBuilder(256);
        requestMessage(request, messageBuilder, -1);
        log(createRequestMarker(request), messageBuilder, request);
    }

    @Override
    public void write(Correlation correlation, HttpRequest request, HttpResponse response) throws IOException {
        StringBuilder messageBuilder = new StringBuilder(256);
        responseMessage(correlation, request, response, messageBuilder, -1);
        log(createResponseMarker(correlation, response), messageBuilder, response);
    }

    private void log(RequestResponseSingleFieldAppendingMarker marker, CharSequence head, HttpMessage httpMessage) {
        CompactRequestResponse message = new CompactRequestResponse(head, CompactRequestResponse.DEFAULT_EXCLUDED_HTTP_HEADERS);
        for (Map.Entry<String, List<String>> entry : httpMessage.getHeaders().entrySet()) {
            message.header(entry.getKey(), String.join(", ", entry.getValue()));
        }
        String contentType = httpMessage.getContentType();
        if (isTextMediaType(contentType)) {
            String body = readBody(httpMessage);
            if (body != null && !body.isBlank()) {
                message.body(CompactRequestResponse.singleLineBody(body, ContentType.isJsonMediaType(contentType)));
            }
        }
        CompactMessageMarker compactMarker = message.getMarker();
        Marker logMarker = compactMarker;
        if (marker != null) {
            // keep the request-response marker (JSON fields used by assertions)
            marker.add(compactMarker);
            logMarker = marker;
        }
        logConsumer.accept(logMarker, message.getMessage());
    }

    private static String readBody(HttpMessage httpMessage) {
        try {
            return httpMessage.getBodyAsString();
        } catch (IOException | RuntimeException e) {
            // log headers, method and status without the body
            return null;
        }
    }

    /** JSON, XML, <code>text/*</code> and form data; not binary content. A missing content type is treated as text. */
    static boolean isTextMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        if (ContentType.isJsonMediaType(contentType) || isXmlMediaType(contentType)) {
            return true;
        }
        String lowerCase = contentType.toLowerCase(Locale.ROOT).strip();
        return lowerCase.startsWith("text/") || lowerCase.startsWith("application/x-www-form-urlencoded");
    }
}
