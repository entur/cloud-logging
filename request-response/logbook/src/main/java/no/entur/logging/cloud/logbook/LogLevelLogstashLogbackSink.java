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

    public LogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, WellformedRequestBodyDecisionSupplier requestBodyWellformedDecisionSupplier, WellformedResponseBodyDecisionSupplier responseBodyWellformedDecisionSupplier, int maxBodySize, int maxSize) {
        super(logConsumer, logLevelEnabled, requestBodyWellformedDecisionSupplier, responseBodyWellformedDecisionSupplier, maxBodySize, maxSize);
    }

    public Marker createRequestMarker(HttpRequest request) {

        // trust our own data
        BooleanSupplier wellformed;
        if(request.getOrigin().equals("local")) {
            wellformed = () -> true;
        } else {
            wellformed = requestBodyWellformedDecisionSupplier.get();
        }

        return new RequestSingleFieldAppendingMarker(request, wellformed, maxBodySize, maxSize);
    }

    public Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        // trust our own data
        BooleanSupplier wellformed;
        if(response.getOrigin().equals("local")) {
            wellformed = () -> true;
        } else {
            wellformed = responseBodyWellformedDecisionSupplier.get();
        }

        return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), wellformed, maxBodySize, maxSize);
    }

}
