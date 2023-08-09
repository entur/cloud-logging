package no.entur.logging.cloud.logbook;

import org.slf4j.Marker;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Sink;

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

}
