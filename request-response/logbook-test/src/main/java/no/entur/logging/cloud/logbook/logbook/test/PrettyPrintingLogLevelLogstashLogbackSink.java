package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logbook.AbstractLogLevelLogstashLogbackSink;

import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import no.entur.logging.cloud.logbook.RequestSingleFieldAppendingMarker;
import no.entur.logging.cloud.logbook.ResponseSingleFieldAppendingMarker;
import no.entur.logging.cloud.logbook.WellformedRequestBodyDecisionSupplier;
import no.entur.logging.cloud.logbook.WellformedResponseBodyDecisionSupplier;
import org.slf4j.Marker;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public class PrettyPrintingLogLevelLogstashLogbackSink extends AbstractLogLevelLogstashLogbackSink {


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public PrettyPrintingLogLevelLogstashLogbackSink build() {
            if(maxBodySize == -1) {
                throw new IllegalStateException("Expected max body size");
            }
            if(maxSize == -1) {
                throw new IllegalStateException("Expected max size");
            }
            if(logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if(level == null) {
                throw new IllegalStateException("Expected log level");
            }

            return new PrettyPrintingLogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), requestBodyWellformedDecisionSupplier, responseBodyWellformedDecisionSupplier, maxBodySize, maxSize);
        }

    }

    public PrettyPrintingLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, WellformedRequestBodyDecisionSupplier requestBodyWellformedDecisionSupplier, WellformedResponseBodyDecisionSupplier responseBodyWellformedDecisionSupplier, int maxBodySize, int maxSize) {
        super(logConsumer, logLevelEnabled, requestBodyWellformedDecisionSupplier, responseBodyWellformedDecisionSupplier, maxBodySize, maxSize);
    }

    protected Marker createRequestMarker(HttpRequest request) {

        // trust our own data
        BooleanSupplier wellformed;
        if(request.getOrigin().equals("local")) {
            wellformed = () -> true;
        } else {
            wellformed = requestBodyWellformedDecisionSupplier.get();
        }


        return new PrettyPrintingRequestSingleFieldAppendingMarker(request, wellformed, maxBodySize, maxSize);
    }

    protected Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        // trust our own data
        BooleanSupplier wellformed;
        if(response.getOrigin().equals("local")) {
            wellformed = () -> true;
        } else {
            wellformed = responseBodyWellformedDecisionSupplier.get();
        }

        return new PrettyPrintingResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), wellformed, maxBodySize, maxSize);
    }


}
