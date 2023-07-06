package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputType;
import no.entur.logging.cloud.logback.logstash.test.DefaultCompositeConsoleOutputMarker;
import no.entur.logging.cloud.logbook.AbstractLogLevelSink;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpMessage;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.RequestURI;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.slf4j.event.EventConstants.DEBUG_INT;
import static org.slf4j.event.EventConstants.ERROR_INT;
import static org.slf4j.event.EventConstants.INFO_INT;
import static org.slf4j.event.EventConstants.TRACE_INT;
import static org.slf4j.event.EventConstants.WARN_INT;

public class PrettyPrintingSink extends AbstractLogLevelSink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        protected Logger logger;

        protected Level level;

        public Builder withLogLevel(Level level) {
            this.level = level;
            return this;
        }

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public PrettyPrintingSink build() {
            if(logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if(level == null) {
                throw new IllegalStateException("Expected log level");
            }

            return new PrettyPrintingSink(logEnabledToBooleanSupplier(), loggerToConsumer(), new JsonFactory());
        }

        protected BiConsumer<Marker, String> loggerToConsumer() {

            int levelInt = level.toInt();
            switch (levelInt) {
                case (TRACE_INT):
                    return logger::trace;
                case (DEBUG_INT):
                    return  logger::debug;
                case (INFO_INT):
                    return logger::info;
                case (WARN_INT):
                    return  logger::warn;
                case (ERROR_INT):
                    return logger::error;
                default:
                    throw new IllegalStateException("Level [" + level + "] not recognized.");
            }

        }

        protected BooleanSupplier logEnabledToBooleanSupplier() {
            int levelInt = level.toInt();
            switch (levelInt) {
                case (TRACE_INT):
                    return logger::isTraceEnabled;
                case (DEBUG_INT):
                    return logger::isDebugEnabled;
                case (INFO_INT):
                    return logger::isInfoEnabled;
                case (WARN_INT):
                    return logger::isWarnEnabled;
                case (ERROR_INT):
                    return logger::isErrorEnabled;
                default:
                    throw new IllegalStateException("Level [" + level + "] not recognized.");
            }
        }

    }
    protected final BiConsumer<Marker, String> logBiConsumer;
    protected final JsonFactory jsonFactory;

    public PrettyPrintingSink(BooleanSupplier logLevelEnabled, BiConsumer<Marker, String> logBiConsumer, JsonFactory jsonFactory) {
        super(logLevelEnabled);
        this.logBiConsumer = logBiConsumer;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public void write(Precorrelation precorrelation, HttpRequest request) throws IOException {
        final String body = request.getBodyAsString();

        final StringBuilder result = new StringBuilder(body.length() + 2048);

        requestMessage(request, result);
        result.append('\n');
        writeHeaders(request.getHeaders(), result);
        writeBody(body, request.getContentType(), result);

        logBiConsumer.accept(DefaultCompositeConsoleOutputMarker.getCurrentCompositeOutput(), result.toString());
    }

    public void write(Correlation correlation, HttpRequest request, HttpResponse response) throws IOException {
        final String body = response.getBodyAsString();

        final StringBuilder result = new StringBuilder(body.length() + 2048);

        responseMessage(request, response, result);
        result.append(" (in ");
        result.append(correlation.getDuration().toMillis());
        result.append(" ms)\n");

        writeHeaders(response.getHeaders(), result);
        writeBody(body, response.getContentType(), result);

        logBiConsumer.accept(DefaultCompositeConsoleOutputMarker.getCurrentCompositeOutput(), result.toString());
    }

    private void writeHeaders(final Map<String, List<String>> headers, final StringBuilder output) {
        if (headers.isEmpty()) {
            return;
        }

        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            output.append(entry.getKey());
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
                generator.useDefaultPrettyPrinter();
                while (parser.nextToken() != null) {
                    generator.copyCurrentEvent(parser);
                }
                generator.flush();
                return writer.toString();
            } catch (IOException e) {
                // ignore, keep payload as-is
            }
        }
        return body;
    }


}
