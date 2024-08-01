package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.github.skjolber.jackson.jsh.AnsiSyntaxHighlight;
import com.github.skjolber.jackson.jsh.SyntaxHighlighter;
import com.github.skjolber.jackson.jsh.SyntaxHighlightingJsonGenerator;
import no.entur.logging.cloud.logbook.AbstractLogLevelSink;
import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import org.slf4j.Marker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Pretty printing + coloring of request-response logging */

public class PrettyPrintingSink extends AbstractLogLevelSink {

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        protected SyntaxHighlighter syntaxHighlighter;

        public Builder withSyntaxHighlighter(SyntaxHighlighter syntaxHighlighter) {
            this.syntaxHighlighter = syntaxHighlighter;
            return this;
        }

        public PrettyPrintingSink build() {
            if(logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if(level == null) {
                throw new IllegalStateException("Expected log level");
            }
            if(syntaxHighlighter == null) {
                throw new IllegalStateException("Expected Json syntax highlighter level");
            }
            if(jsonFactory == null) {
                jsonFactory = new JsonFactory();
            }

            // TODO what about max size here? Max size is really a function of the logging backend, but getting the same during testing as in production makes most sense

            return new PrettyPrintingSink(logEnabledToBooleanSupplier(), loggerToBiConsumer(), jsonFactory, syntaxHighlighter);
        }

    }
    protected final JsonFactory jsonFactory;

    protected final SyntaxHighlighter syntaxHighlighter;

    public PrettyPrintingSink(BooleanSupplier logLevelEnabled, BiConsumer<Marker, String> logConsumer, JsonFactory jsonFactory, SyntaxHighlighter syntaxHighlighter) {
        super(logLevelEnabled, logConsumer);
        this.jsonFactory = jsonFactory;
        this.syntaxHighlighter = syntaxHighlighter;
    }

    @Override
    protected void requestMessage(HttpRequest request, StringBuilder messageBuilder) throws IOException {
        super.requestMessage(request, messageBuilder);

        final String body = request.getBodyAsString();

        messageBuilder.append('\n');
        writeHeaders(request.getHeaders(), messageBuilder);
        writeBody(body, request.getContentType(), messageBuilder);
    }

    @Override
    protected void responseMessage(Correlation correlation, HttpRequest request, HttpResponse response, StringBuilder messageBuilder) throws IOException {
        super.responseMessage(correlation, request, response, messageBuilder);

        final String body = response.getBodyAsString();

        messageBuilder.append('\n');
        writeHeaders(response.getHeaders(), messageBuilder);
        if(body != null) {
            writeBody(body, response.getContentType(), messageBuilder);
        }

    }


    private void writeHeaders(final Map<String, List<String>> headers, final StringBuilder output) {
        if (headers.isEmpty()) {
            return;
        }

        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            output.append(AnsiSyntaxHighlight.ESC_START);
            output.append(AnsiSyntaxHighlight.CYAN);
            output.append(AnsiSyntaxHighlight.ESC_END);
            output.append(entry.getKey().toLowerCase());
            output.append(AnsiSyntaxHighlight.ESC_START);
            output.append(AnsiSyntaxHighlight.CLEAR);
            output.append(AnsiSyntaxHighlight.ESC_END);

            output.append(": ");
            final List<String> headerValues = entry.getValue();
            if (!headerValues.isEmpty()) {
                for (final String value : entry.getValue()) {
                    output.append(value);
                    output.append(", ");
                }
                output.setLength(output.length() - 2); // discard last comma
            }
            output.append('\n');
        }
    }

    private void writeBody(final String body, String contentType, final StringBuilder output) {
        if (!body.isEmpty()) {
            output.append('\n');
            if(ContentType.isJsonMediaType(contentType)) {
                output.append(prettyPrint(body));
            } else {
                output.append(body);
            }
        } else {
            output.setLength(output.length() - 1); // discard last newline
        }
    }

    public String prettyPrint(String body) {

        if (body != null && body.length() > 0) {
            try (
                JsonParser parser = jsonFactory.createParser(body);
                StringWriter writer = new StringWriter(body.length() * 2);
                JsonGenerator generator = jsonFactory.createGenerator(writer);
            ) {
                JsonGenerator jsonGenerator = new SyntaxHighlightingJsonGenerator(generator, syntaxHighlighter,true);
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
