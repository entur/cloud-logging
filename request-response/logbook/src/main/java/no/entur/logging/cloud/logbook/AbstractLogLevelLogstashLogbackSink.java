package no.entur.logging.cloud.logbook;

import org.slf4j.Marker;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelLogstashLogbackSink extends AbstractLogLevelSink {

    protected final BooleanSupplier requestBodyWellformedDecisionSupplier;
    protected final BooleanSupplier responseBodyWellformedDecisionSupplier;

    protected final int maxBodySize;
    protected final int maxSize;

    public AbstractLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, BooleanSupplier requestBodyWellformedDecisionSupplier, BooleanSupplier responseBodyWellformedDecisionSupplier, int maxBodySize, int maxSize) {
        super(logLevelEnabled, logConsumer);
        this.requestBodyWellformedDecisionSupplier = requestBodyWellformedDecisionSupplier;
        this.responseBodyWellformedDecisionSupplier = responseBodyWellformedDecisionSupplier;
        this.maxBodySize = maxBodySize;
        this.maxSize = maxSize;
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

    protected abstract Marker createResponseMarker(Correlation correlation, HttpResponse response);

    protected abstract Marker createRequestMarker(HttpRequest request);

}
