package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputMarker;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputType;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import org.zalando.logbook.HttpRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ConsoleOutputTypeRequestMarker extends RequestSingleFieldAppendingMarker implements CompositeConsoleOutputMarker {

    private CompositeConsoleOutputType compositeConsoleOutputType;

    public ConsoleOutputTypeRequestMarker(HttpRequest request, boolean validateJsonBody, int maxBodySize, int maxSize) {
        super(request, validateJsonBody, maxBodySize, maxSize);
    }

    @Override
    protected void prepareForDeferredProcessing(HttpRequest message) {
        super.prepareForDeferredProcessing(message);

        compositeConsoleOutputType = CompositeConsoleOutputControl.getOutput();
    }

    @Override
    public CompositeConsoleOutputType getCompositeConsoleOutputType() {
        return compositeConsoleOutputType;
    }
}
