package no.entur.logging.cloud.rr.grpc.test;

import no.entur.logging.cloud.logback.logstash.test.AiLoggingControl;
import no.entur.logging.cloud.rr.grpc.AbstractSinkBuilder;
import no.entur.logging.cloud.rr.grpc.LogbackLogstashGrpcSink;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.slf4j.Marker;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Compact request-response logging message for tooling (AI mode): a single line without pretty-printing or colors,
 * in the form <code>message | header: value; header: value | body</code>.
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

    private final JsonMapper mapper = JsonMapper.builder().build();

    public CompactLogbackLogstashGrpcSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled) {
        super(logConsumer, logLevelEnabled);
    }

    @Override
    protected void connectMessage(GrpcConnect connect, StringBuilder messageBuilder) {
        super.connectMessage(connect, messageBuilder);
        compact(connect.getHeaders(), null, messageBuilder);
    }

    @Override
    protected void requestMessage(GrpcRequest request, StringBuilder messageBuilder) {
        super.requestMessage(request, messageBuilder);
        compact(request.getHeaders(), request.getBody(), messageBuilder);
    }

    @Override
    protected void responseMessage(GrpcResponse response, StringBuilder messageBuilder) {
        super.responseMessage(response, messageBuilder);
        compact(response.getHeaders(), response.getBody(), messageBuilder);
    }

    @Override
    protected void disconnectMessage(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage, StringBuilder messageBuilder) {
        super.disconnectMessage(connectMessage, disconnectMessage, messageBuilder);
        compact(disconnectMessage.getHeaders(), null, messageBuilder);
    }

    private void compact(Map<String, ?> headers, String body, StringBuilder output) {
        String message = output.toString().strip().replaceAll("\\s*\\R\\s*", " ");
        output.setLength(0);
        output.append(message);

        if (headers != null && !headers.isEmpty()) {
            output.append(" | ");
            boolean first = true;
            for (Map.Entry<String, ?> entry : headers.entrySet()) {
                if (!first) {
                    output.append("; ");
                }
                first = false;
                output.append(entry.getKey().toLowerCase()).append(": ").append(entry.getValue());
            }
        }

        if (body != null && !body.isBlank()) {
            String compactBody;
            try {
                compactBody = mapper.readTree(body).toString();
            } catch (Exception e) {
                compactBody = body.strip().replaceAll("\\s*\\R\\s*", "");
            }
            output.append(AiLoggingControl.BODY_MARKER).append(compactBody);
        }
    }
}
