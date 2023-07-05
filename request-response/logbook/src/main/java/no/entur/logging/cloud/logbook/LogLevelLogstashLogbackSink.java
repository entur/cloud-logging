package no.entur.logging.cloud.logbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.zalando.logbook.*;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.slf4j.event.EventConstants.DEBUG_INT;
import static org.slf4j.event.EventConstants.ERROR_INT;
import static org.slf4j.event.EventConstants.INFO_INT;
import static org.slf4j.event.EventConstants.TRACE_INT;
import static org.slf4j.event.EventConstants.WARN_INT;

public class LogLevelLogstashLogbackSink extends AbstractLogLevelLogstashLogbackSink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public LogLevelLogstashLogbackSink build() {
            if(logger == null) {
                logger = LoggerFactory.getLogger("no.entur.logging.cloud.logbook");
            }
            if(level == null) {
                level = Level.INFO;
            }
            return new LogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), validateRequestJsonBody, validateResponseJsonBody);
        }

    }

    public LogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, boolean validateRequestJsonBody, boolean validateResponseJsonBody) {
        super(logConsumer, logLevelEnabled, validateRequestJsonBody, validateResponseJsonBody);
    }


    protected Marker createRequestSingleFieldAppendingMarker(HttpRequest request) {
        return new RequestSingleFieldAppendingMarker(request, validateRequestJsonBody);
    }

    protected Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), validateRequestJsonBody);
    }

}
