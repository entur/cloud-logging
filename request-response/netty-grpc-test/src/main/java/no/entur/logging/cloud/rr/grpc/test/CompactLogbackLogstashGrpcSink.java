package no.entur.logging.cloud.rr.grpc.test;

import no.entur.logging.cloud.logback.logstash.test.CompactRequestResponse;
import no.entur.logging.cloud.logback.logstash.test.CompactMessageMarker;
import no.entur.logging.cloud.rr.grpc.AbstractSinkBuilder;
import no.entur.logging.cloud.rr.grpc.LogbackLogstashGrpcSink;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.slf4j.Marker;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Compact request-response logging message for tooling (AI mode): a single line without pretty-printing or colors,
 * in the form <code>message | header: value; header: value | body: &lt;body&gt;</code>. The compact file gets a shorter variant
 * (see {@link CompactRequestResponse}).
 */
public class CompactLogbackLogstashGrpcSink extends LogbackLogstashGrpcSink {

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public CompactLogbackLogstashGrpcSink build() {
            if (logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if (level == null) {
                throw new IllegalStateException("Expected log level");
            }
            return new CompactLogbackLogstashGrpcSink(loggerToBiConsumer(), logEnabledToBooleanSupplier());
        }
    }

    public CompactLogbackLogstashGrpcSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled) {
        super(logConsumer, logLevelEnabled);
    }

    @Override
    public void connectMessage(GrpcConnect connect) {
        StringBuilder messageBuilder = new StringBuilder(256);
        connectMessage(connect, messageBuilder);
        log(createConnectMarker(connect), messageBuilder, connect.getHeaders(), null);
    }

    @Override
    public void requestMessage(GrpcRequest request) {
        StringBuilder messageBuilder = new StringBuilder(256);
        requestMessage(request, messageBuilder);
        log(createRequestMarker(request), messageBuilder, request.getHeaders(), request.getBody());
    }

    @Override
    public void responseMessage(GrpcResponse response) {
        StringBuilder messageBuilder = new StringBuilder(256);
        responseMessage(response, messageBuilder);
        log(createResponseMarker(response), messageBuilder, response.getHeaders(), response.getBody());
    }

    @Override
    public void disconnectMessage(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage) {
        StringBuilder messageBuilder = new StringBuilder(256);
        disconnectMessage(connectMessage, disconnectMessage, messageBuilder);
        log(createDisconnectMarker(connectMessage, disconnectMessage), messageBuilder, disconnectMessage.getHeaders(), null);
    }

    private void log(Marker marker, CharSequence head, Map<String, ?> headers, String body) {
        CompactRequestResponse message = new CompactRequestResponse(head, CompactRequestResponse.DEFAULT_EXCLUDED_GRPC_HEADERS);
        if (headers != null) {
            for (Map.Entry<String, ?> entry : headers.entrySet()) {
                message.header(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        if (body != null && !body.isBlank()) {
            // gRPC bodies are JSON (protobuf printed as JSON)
            message.body(CompactRequestResponse.singleLineBody(body, true));
        }
        CompactMessageMarker compactMarker = message.getMarker();
        if (marker != null) {
            // keep the request-response marker (JSON fields used by assertions)
            marker.add(compactMarker);
        } else {
            marker = compactMarker;
        }
        logConsumer.accept(marker, message.getMessage());
    }
}
