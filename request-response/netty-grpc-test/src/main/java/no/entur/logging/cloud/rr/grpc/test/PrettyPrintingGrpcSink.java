package no.entur.logging.cloud.rr.grpc.test;

import org.entur.jackson.tools.jsh.AnsiSyntaxHighlight;
import org.entur.jackson.tools.jsh.SyntaxHighlighter;
import org.entur.jackson.tools.jsh.SyntaxHighlightingJsonGenerator;
import org.entur.jackson.tools.jsh.SyntaxHighlightingPrettyPrinter;
import tools.jackson.core.JsonGenerator;
import no.entur.logging.cloud.rr.grpc.AbstractSinkBuilder;
import no.entur.logging.cloud.rr.grpc.LogbackLogstashGrpcSink;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.slf4j.Marker;
import tools.jackson.core.JsonParser;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Pretty printing + coloring of request-response logging message. */

public class PrettyPrintingGrpcSink extends LogbackLogstashGrpcSink {

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        protected SyntaxHighlighter syntaxHighlighter;

        public Builder withSyntaxHighlighter(SyntaxHighlighter syntaxHighlighter) {
            this.syntaxHighlighter = syntaxHighlighter;
            return this;
        }

        public PrettyPrintingGrpcSink build() {
            if(logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if(level == null) {
                throw new IllegalStateException("Expected log level");
            }
            if(syntaxHighlighter == null) {
                throw new IllegalStateException("Expected Json syntax highlighter level");
            }

            JsonMapper mapper = JsonMapper.builder().defaultPrettyPrinter(new SyntaxHighlightingPrettyPrinter(syntaxHighlighter))
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .build();

            return new PrettyPrintingGrpcSink(logEnabledToBooleanSupplier(), loggerToBiConsumer(), mapper, syntaxHighlighter);
        }

    }

    protected final BiConsumer<Marker, String> logConsumer;
    protected final JsonMapper mapper;

    protected final SyntaxHighlighter syntaxHighlighter;

    public PrettyPrintingGrpcSink(BooleanSupplier logLevelEnabled, BiConsumer<Marker, String> logConsumer, JsonMapper mapper, SyntaxHighlighter syntaxHighlighter) {
        super(logConsumer, logLevelEnabled);
        this.logConsumer = logConsumer;
        this.mapper = mapper;
        this.syntaxHighlighter = syntaxHighlighter;
    }

    @Override
    public void connectMessage(GrpcConnect connect) {
        final StringBuilder messageBuilder = new StringBuilder(2048);

       connectMessage(connect, messageBuilder);

        logConsumer.accept(createConnectMarker(connect), messageBuilder.toString());
    }

    @Override
    protected void connectMessage(GrpcConnect connect, StringBuilder messageBuilder) {
        super.connectMessage(connect, messageBuilder);
        Map<String, ?> headers = connect.getHeaders();
        if(headers != null && !headers.isEmpty()) {
            messageBuilder.append('\n');
            writeHeaders(headers, messageBuilder);
        }
    }

    @Override
    public void requestMessage(GrpcRequest message) {
        String body = message.getBody();
        final StringBuilder result = new StringBuilder(body != null ? body.length() + 2048 : 2048);

        requestMessage(message, result);

        logConsumer.accept(createRequestMarker(message), result.toString());
    }

    @Override
    protected void requestMessage(GrpcRequest request, StringBuilder messageBuilder) {
        super.requestMessage(request, messageBuilder);

        String body = request.getBody();
        Map<String, ?> headers = request.getHeaders();
        if(headers != null && !headers.isEmpty()) {
            messageBuilder.append('\n');
            writeHeaders(headers, messageBuilder);
        }

        if(body != null) {
            writeBody(body, messageBuilder);
        }
    }

    @Override
    protected void responseMessage(GrpcResponse response, StringBuilder messageBuilder) {
        super.responseMessage(response, messageBuilder);

        Map<String, ?> headers = response.getHeaders();
        if(headers != null && !headers.isEmpty()) {
            messageBuilder.append('\n');
            writeHeaders(headers, messageBuilder);
        }

        String body = response.getBody();
        if(body != null) {
            writeBody(body, messageBuilder);
        }
    }

    @Override
    protected void disconnectMessage(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage, StringBuilder messageBuilder) {
        super.disconnectMessage(connectMessage, disconnectMessage, messageBuilder);

        Map<String, ?> headers = disconnectMessage.getHeaders();
        if(headers != null && !headers.isEmpty()) {
            messageBuilder.append('\n');
            writeHeaders(headers, messageBuilder);
        }
    }

    private void writeHeaders(final Map<String, ?> headers, final StringBuilder output) {
        if (headers.isEmpty()) {
            return;
        }

        for (final Map.Entry<String, ?> entry : headers.entrySet()) {
            output.append(AnsiSyntaxHighlight.ESC_START);
            output.append(AnsiSyntaxHighlight.CYAN);
            output.append(AnsiSyntaxHighlight.ESC_END);
            output.append(entry.getKey().toLowerCase());
            output.append(AnsiSyntaxHighlight.ESC_START);
            output.append(AnsiSyntaxHighlight.CLEAR);
            output.append(AnsiSyntaxHighlight.ESC_END);

            output.append(": ");
            final Object value = entry.getValue();
            output.append(value);
            output.append('\n');
        }
        output.setLength(output.length() - 1);
    }

    private void writeBody(final String body, final StringBuilder output) {
        if (!body.isEmpty()) {
            output.append('\n');
            output.append(prettyPrint(body));
        } else {
            output.setLength(output.length() - 1); // discard last newline
        }
    }

    public String prettyPrint(String body) {

        if (body != null && body.length() > 0) {
            try (
                    JsonParser parser = mapper.createParser(body);
                    StringWriter writer = new StringWriter(body.length() * 2);
                    JsonGenerator generator = mapper.createGenerator(writer);
            ) {
                SyntaxHighlightingPrettyPrinter prettyPrinter = (SyntaxHighlightingPrettyPrinter) generator.getPrettyPrinter();

                JsonGenerator jsonGenerator = new SyntaxHighlightingJsonGenerator(generator, prettyPrinter, prettyPrinter.getObjectIndenter(), prettyPrinter.getArrayIndenter(), prettyPrinter.getSyntaxHighlighter());
                while (parser.nextToken() != null) {
                    jsonGenerator.copyCurrentEvent(parser);
                }
                jsonGenerator.flush();
                return writer.toString();
            } catch (IOException e) {
                // ignore, keep payload as-is
            }
        }
        return body;
    }

}
