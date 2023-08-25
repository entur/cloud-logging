package no.entur.logging.cloud.logbook;

import org.slf4j.Marker;
import org.zalando.logbook.*;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public class LogLevelLogstashLogbackSink extends AbstractLogLevelLogstashLogbackSink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public LogLevelLogstashLogbackSink build() {
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
            return new LogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), requestBodyWellformedDecisionSupplier, responseBodyWellformedDecisionSupplier, maxBodySize, maxSize);
        }
    }

    public LogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, BooleanSupplier requestBodyWellformedDecisionSupplier, BooleanSupplier responseBodyWellformedDecisionSupplier, int maxBodySize, int maxSize) {
        super(logConsumer, logLevelEnabled, requestBodyWellformedDecisionSupplier, responseBodyWellformedDecisionSupplier, maxBodySize, maxSize);
    }

    public Marker createRequestMarker(HttpRequest request) {
        return new RequestSingleFieldAppendingMarker(request, requestBodyWellformedDecisionSupplier.getAsBoolean(), maxBodySize, maxSize);
    }

    public Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), requestBodyWellformedDecisionSupplier.getAsBoolean(), maxBodySize, maxSize);
    }

}
