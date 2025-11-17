package no.entur.logging.cloud.logbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.zalando.logbook.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelSink implements Sink {

    private final static Logger logger = LoggerFactory.getLogger(AbstractLogLevelSink.class);

    public static boolean isXmlMediaType(@Nullable final String contentType) {
        if (contentType == null) {
            return false;
        }

        String contentTypeWithoutEncoding;
        // text/xml;charset=UTF-8
        int index = contentType.indexOf(';');
        if (index == -1) {
            contentTypeWithoutEncoding = contentType;
        } else {
            contentTypeWithoutEncoding = contentType.substring(0, index).trim();
        }

        final String lowerCasedContentType = contentTypeWithoutEncoding.toLowerCase();

        boolean isApplicationOrText = lowerCasedContentType.startsWith("application/")
                || lowerCasedContentType.startsWith("text/");
        if (!isApplicationOrText) {
            return false;
        }

        return lowerCasedContentType.endsWith("+xml") || lowerCasedContentType.endsWith("/xml");
    }

    protected final BooleanSupplier logLevelEnabled;

    protected final BiConsumer<Marker, String> logConsumer;

    protected final MessageComposer server;
    protected final MessageComposer client;

    public AbstractLogLevelSink(BooleanSupplier logLevelEnabled, BiConsumer<Marker, String> logConsumer,
            MessageComposer server, MessageComposer client) {
        this.logLevelEnabled = logLevelEnabled;
        this.logConsumer = logConsumer;
        this.server = server;
        this.client = client;
    }

    @Override
    public boolean isActive() {
        return logLevelEnabled.getAsBoolean();
    }

    protected void requestMessage(HttpRequest request, StringBuilder messageBuilder) throws IOException {
        if (request.getOrigin() == Origin.LOCAL) {
            client.requestMessage(request, messageBuilder);
        } else {
            server.requestMessage(request, messageBuilder);
        }
    }

    protected void responseMessage(Correlation correlation, HttpRequest request, HttpResponse response,
            StringBuilder messageBuilder) throws IOException {
        if (request.getOrigin() == Origin.LOCAL) {
            client.responseMessage(correlation, request, response, messageBuilder);
        } else {
            server.responseMessage(correlation, request, response, messageBuilder);
        }
    }

    @Override
    public void write(final Precorrelation precorrelation, final HttpRequest request) throws IOException {
        try {
            Marker marker = createRequestMarker(request);
            StringBuilder stringBuilder = new StringBuilder(256);
            requestMessage(request, stringBuilder);
            logConsumer.accept(marker, stringBuilder.toString());
        } catch(Throwable e) {
            logger.warn("Unexpected problem writing request message", e);
        }
    }

    public void write(Correlation correlation, final HttpRequest request, HttpResponse response) throws IOException {
        try {
            Marker marker = createResponseMarker(correlation, response);
            StringBuilder stringBuilder = new StringBuilder(256);
            responseMessage(correlation, request, response, stringBuilder);
            logConsumer.accept(marker, stringBuilder.toString());
        } catch(Throwable e) {
            logger.warn("Unexpected problem writing response message", e);
        }
    }

    protected Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        return null;
    }

    protected Marker createRequestMarker(HttpRequest request) {
        return null;
    }

}
