package no.entur.logging.cloud.logbook;

import org.slf4j.Marker;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.Sink;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelSink implements Sink {

    protected final BooleanSupplier logLevelEnabled;

    public AbstractLogLevelSink(BooleanSupplier logLevelEnabled) {
        this.logLevelEnabled = logLevelEnabled;
    }

    @Override public boolean isActive() {
        return logLevelEnabled.getAsBoolean();
    }

    protected void requestMessage(HttpRequest request, StringBuilder messageBuilder) {
        messageBuilder.append(request.getMethod());
        messageBuilder.append(' ');
        messageBuilder.append(request.getRequestUri());
    }

    protected void responseMessage(HttpRequest request, HttpResponse response, StringBuilder messageBuilder) {
        final String requestUri = request.getRequestUri();
        messageBuilder.append(response.getStatus());
        final String reasonPhrase = response.getReasonPhrase();
        if (reasonPhrase != null) {
            messageBuilder.append(' ');
            messageBuilder.append(reasonPhrase);
        }
        messageBuilder.append(' ');
        messageBuilder.append(requestUri);
    }

}
