package no.entur.logging.cloud.logbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.Sink;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.slf4j.event.EventConstants.DEBUG_INT;
import static org.slf4j.event.EventConstants.ERROR_INT;
import static org.slf4j.event.EventConstants.INFO_INT;
import static org.slf4j.event.EventConstants.TRACE_INT;
import static org.slf4j.event.EventConstants.WARN_INT;

public abstract class AbstractLogLevelLogstashLogbackSink implements Sink {

    protected final BiConsumer<Marker, String> logConsumer;
    protected final BooleanSupplier logLevelEnabled;

    protected final boolean validateRequestJsonBody;
    protected final boolean validateResponseJsonBody;

    public AbstractLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, boolean validateRequestJsonBody, boolean validateResponseJsonBody) {
        this.logConsumer = logConsumer;
        this.logLevelEnabled = logLevelEnabled;
        this.validateRequestJsonBody = validateRequestJsonBody;
        this.validateResponseJsonBody = validateResponseJsonBody;
    }

    @Override public boolean isActive() {
        return logLevelEnabled.getAsBoolean();
    }

    @Override
    public void write(final Precorrelation precorrelation, final HttpRequest request) throws IOException {
        Marker marker = createRequestSingleFieldAppendingMarker(request);
        logConsumer.accept (marker, requestMessage(request));
    }

    protected abstract Marker createRequestSingleFieldAppendingMarker(HttpRequest request);

    private String requestMessage(HttpRequest request) {
        return request.getMethod() + " " + request.getRequestUri();
    }

    public void write(Correlation correlation, final HttpRequest request, HttpResponse response) throws IOException {
        Marker marker = createResponseMarker(correlation, response);
        logConsumer.accept (marker,  responseMessage(request, response));
    }

    protected abstract Marker createResponseMarker(Correlation correlation, HttpResponse response);

    protected String responseMessage(HttpRequest request, HttpResponse response) {
        final String requestUri = request.getRequestUri();
        final StringBuilder messageBuilder = new StringBuilder(64 + requestUri.length());
        messageBuilder.append(response.getStatus());
        final String reasonPhrase = response.getReasonPhrase();
        if (reasonPhrase != null) {
            messageBuilder.append(' ');
            messageBuilder.append(reasonPhrase);
        }
        messageBuilder.append(' ');
        messageBuilder.append(request.getMethod());
        messageBuilder.append(' ');
        messageBuilder.append(requestUri);

        return messageBuilder.toString();
    }

}
