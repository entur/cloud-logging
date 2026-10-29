package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logbook.AbstractLogLevelSink;
import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import no.entur.logging.cloud.logbook.MessageComposer;
import org.slf4j.Marker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Compact request-response logging message for tooling (AI mode): a single line without pretty-printing or colors,
 * in the form <code>message | header: value; header: value | body</code>.
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
    protected void requestMessage(HttpRequest request, StringBuilder messageBuilder, int truncated) throws IOException {
        super.requestMessage(request, messageBuilder, truncated);
        compact(request.getHeaders(), request.getContentType(), request.getBodyAsString(), messageBuilder);
    }

    @Override
    protected void responseMessage(Correlation correlation, HttpRequest request, HttpResponse response,
                                   StringBuilder messageBuilder, int truncated) throws IOException {
        super.responseMessage(correlation, request, response, messageBuilder, truncated);
        compact(response.getHeaders(), response.getContentType(), response.getBodyAsString(), messageBuilder);
    }

    private void compact(Map<String, List<String>> headers, String contentType, String body, StringBuilder output) {
        String message = output.toString().strip().replaceAll("\\s*\\R\\s*", " ");
        output.setLength(0);
        output.append(message);

        if (!headers.isEmpty()) {
            output.append(" | ");
            boolean first = true;
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (!first) {
                    output.append("; ");
                }
                first = false;
                output.append(entry.getKey().toLowerCase()).append(": ").append(String.join(", ", entry.getValue()));
            }
        }

        boolean json = ContentType.isJsonMediaType(contentType);
        if ((json || isXmlMediaType(contentType)) && body != null && !body.isBlank()) {
            output.append(" | ").append(compactBody(body, json));
        }
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
