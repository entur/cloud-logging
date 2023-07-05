package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logbook.AbstractLogLevelLogstashLogbackSink;

import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
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
            if(logger == null) {
                logger = LoggerFactory.getLogger("no.entur.logging.cloud.logbook");
            }
            if(level == null) {
                level = Level.INFO;
            }
            return new PrettyPrintingLogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), validateRequestJsonBody, validateResponseJsonBody);
        }

    }

    public PrettyPrintingLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, boolean validateRequestJsonBody, boolean validateResponseJsonBody) {
        super(logConsumer, logLevelEnabled, validateRequestJsonBody, validateResponseJsonBody);
    }

    protected Marker createRequestSingleFieldAppendingMarker(HttpRequest request) {
        return new PrettyPrintingRequestSingleFieldAppendingMarker(request, validateRequestJsonBody);
    }

    protected Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        return new PrettyPrintingResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), validateRequestJsonBody);
    }

}
