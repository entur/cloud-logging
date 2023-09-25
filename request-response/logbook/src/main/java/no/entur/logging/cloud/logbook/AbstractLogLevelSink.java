package no.entur.logging.cloud.logbook;

import org.slf4j.Marker;
import org.zalando.logbook.*;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelSink implements Sink {

    protected final BooleanSupplier logLevelEnabled;

    protected final BiConsumer<Marker, String> logConsumer;

    public AbstractLogLevelSink(BooleanSupplier logLevelEnabled, BiConsumer<Marker, String> logConsumer) {
        this.logLevelEnabled = logLevelEnabled;
        this.logConsumer = logConsumer;
    }

    @Override public boolean isActive() {
        return logLevelEnabled.getAsBoolean();
    }

    protected void requestMessage(HttpRequest request, StringBuilder messageBuilder) throws IOException {
        messageBuilder.append(request.getMethod());
        messageBuilder.append(' ');
        messageBuilder.append(request.getRequestUri());
    }

    protected void responseMessage(Correlation correlation, HttpRequest request, HttpResponse response, StringBuilder messageBuilder) throws IOException {
        final String requestUri = request.getRequestUri();
        messageBuilder.append(response.getStatus());
        final String reasonPhrase = response.getReasonPhrase();
        if (reasonPhrase != null) {
            messageBuilder.append(' ');
            messageBuilder.append(reasonPhrase);
        }
        messageBuilder.append(' ');
        messageBuilder.append(requestUri);

        messageBuilder.append(" (in ");
        messageBuilder.append(correlation.getDuration().toMillis());
        messageBuilder.append(" ms)");
    }

    @Override
    public void write(final Precorrelation precorrelation, final HttpRequest request) throws IOException {
        Marker marker = createRequestMarker(request);
        StringBuilder stringBuilder = new StringBuilder(256);
        requestMessage(request, stringBuilder);
        logConsumer.accept (marker, stringBuilder.toString());
    }

    public void write(Correlation correlation, final HttpRequest request, HttpResponse response) throws IOException {
        Marker marker = createResponseMarker(correlation, response);
        StringBuilder stringBuilder = new StringBuilder(256);
        responseMessage(correlation, request, response, stringBuilder);
        logConsumer.accept(marker, stringBuilder.toString());
    }

    protected Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        return null;
    }

    protected Marker createRequestMarker(HttpRequest request) {
        return null;
    }

}
