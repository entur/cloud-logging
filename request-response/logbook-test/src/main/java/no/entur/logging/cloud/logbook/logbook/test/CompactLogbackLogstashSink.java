package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logback.logstash.test.CompactRequestResponse;
import no.entur.logging.cloud.logbook.AbstractLogLevelSink;
import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import no.entur.logging.cloud.logbook.MessageComposer;
import org.slf4j.Marker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Compact request-response logging message for tooling (AI mode): a single line without pretty-printing or colors,
 * in the form <code>message | header: value; header: value | body: &lt;body&gt;</code>. The compact file gets a shorter variant
 * (see {@link CompactRequestResponse}).
 */
public class CompactLogbackLogstashSink extends AbstractLogLevelSink {

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public CompactLogbackLogstashSink build() {
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
            return new CompactLogbackLogstashSink(logEnabledToBooleanSupplier(), loggerToBiConsumer(), server, client);
        }
    }

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public CompactLogbackLogstashSink(BooleanSupplier logLevelEnabled, BiConsumer<Marker, String> logConsumer,
                                      MessageComposer server, MessageComposer client) {
        super(logLevelEnabled, logConsumer, server, client);
    }

    @Override
    public void write(Precorrelation precorrelation, HttpRequest request) throws IOException {
        StringBuilder messageBuilder = new StringBuilder(256);
        requestMessage(request, messageBuilder, -1);
        log(messageBuilder, request.getHeaders(), request.getContentType(), request.getBodyAsString());
    }

    @Override
    public void write(Correlation correlation, HttpRequest request, HttpResponse response) throws IOException {
        StringBuilder messageBuilder = new StringBuilder(256);
        responseMessage(correlation, request, response, messageBuilder, -1);
        log(messageBuilder, response.getHeaders(), response.getContentType(), response.getBodyAsString());
    }

    private void log(CharSequence head, Map<String, List<String>> headers, String contentType, String body) {
        CompactRequestResponse message = new CompactRequestResponse(head, CompactRequestResponse.DEFAULT_EXCLUDED_HTTP_HEADERS);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            message.header(entry.getKey(), String.join(", ", entry.getValue()));
        }
        boolean json = ContentType.isJsonMediaType(contentType);
        if ((json || isXmlMediaType(contentType)) && body != null && !body.isBlank()) {
            message.body(compactBody(body, json));
        }
        logConsumer.accept(message.getMarker(), message.getMessage());
    }

    private String compactBody(String body, boolean json) {
        if (json) {
            try {
                return jsonMapper.readTree(body).toString();
            } catch (Exception e) {
                // fall through, collapse whitespace instead
            }
        }
        return body.strip().replaceAll("\\s*\\R\\s*", "");
    }
}
