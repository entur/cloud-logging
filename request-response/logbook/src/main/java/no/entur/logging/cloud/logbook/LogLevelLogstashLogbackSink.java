package no.entur.logging.cloud.logbook;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
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
            return new LogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), validateRequestJsonBody, validateResponseJsonBody, maxBodySize, maxSize);
        }
    }

    public LogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, boolean validateRequestJsonBody, boolean validateResponseJsonBody, int maxBodySize, int maxSize) {
        super(logConsumer, logLevelEnabled, validateRequestJsonBody, validateResponseJsonBody, maxBodySize, maxSize);
    }


    protected Marker createRequestSingleFieldAppendingMarker(HttpRequest request) {
        return new RequestSingleFieldAppendingMarker(request, validateRequestJsonBody, maxBodySize, maxSize);
    }

    protected Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), validateRequestJsonBody, maxBodySize, maxSize);
    }

}
