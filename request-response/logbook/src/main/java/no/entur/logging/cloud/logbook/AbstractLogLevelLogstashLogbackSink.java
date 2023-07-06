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

public abstract class AbstractLogLevelLogstashLogbackSink extends AbstractLogLevelSink {

    protected final BiConsumer<Marker, String> logConsumer;

    protected final boolean validateRequestJsonBody;
    protected final boolean validateResponseJsonBody;

    protected final int maxBodySize;
    protected final int maxSize;

    public AbstractLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, boolean validateRequestJsonBody, boolean validateResponseJsonBody, int maxBodySize, int maxSize) {
        super(logLevelEnabled);

        this.logConsumer = logConsumer;
        this.validateRequestJsonBody = validateRequestJsonBody;
        this.validateResponseJsonBody = validateResponseJsonBody;
        this.maxBodySize = maxBodySize;
        this.maxSize = maxSize;
    }

    @Override
    public void write(final Precorrelation precorrelation, final HttpRequest request) throws IOException {
        Marker marker = createRequestSingleFieldAppendingMarker(request);
        StringBuilder stringBuilder = new StringBuilder(256);
        requestMessage(request, stringBuilder);
        logConsumer.accept (marker, stringBuilder.toString());
    }

    protected abstract Marker createRequestSingleFieldAppendingMarker(HttpRequest request);

    public void write(Correlation correlation, final HttpRequest request, HttpResponse response) throws IOException {
        try {
            Marker marker = createResponseMarker(correlation, response);
            StringBuilder stringBuilder = new StringBuilder(256);
            responseMessage(request, response, stringBuilder);
            logConsumer.accept(marker, stringBuilder.toString());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract Marker createResponseMarker(Correlation correlation, HttpResponse response);


}
