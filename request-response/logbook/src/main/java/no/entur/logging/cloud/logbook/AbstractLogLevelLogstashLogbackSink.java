package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.util.JsonValidator;
import no.entur.logging.cloud.logbook.util.MaxSizeJsonFilter;
import org.slf4j.Marker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelLogstashLogbackSink extends AbstractLogLevelSink {
    
    protected final MaxSizeJsonFilter maxSizeJsonFilter;
    protected final JsonValidator jsonValidator;

    protected final int maxSize;

    protected final RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier;

    public AbstractLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, JsonFactory jsonFactory, int maxSize, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        super(logLevelEnabled, logConsumer);

        this.maxSize = maxSize;
        this.maxSizeJsonFilter = new MaxSizeJsonFilter(maxSize, jsonFactory);
        this.jsonValidator = new JsonValidator(jsonFactory);

        this.remoteHttpMessageContextSupplier = remoteHttpMessageContextSupplier;
    }

    public Marker createRequestMarker(HttpRequest request) {

        if(!ContentType.isJsonMediaType(request.getContentType())) {
            return newRequestSingleFieldAppendingMarker(request, null, false);
        }

        String bodyAsString;
        try {
            bodyAsString = request.getBodyAsString();
        } catch(Exception e){
            return new RequestSingleFieldAppendingMarker(request, null, false);
        }

        if(bodyAsString == null || bodyAsString.length() == 0) {
            return newRequestSingleFieldAppendingMarker(request, null, false);
        }

        String body = null;
        boolean wellformed;

        if (request.getOrigin().equals("local")) {
            // trust data from ourselves to be wellformed
            if(bodyAsString.length() > maxSize) {
                try {
                    body = maxSizeJsonFilter.transform(bodyAsString);
                    wellformed = true;
                } catch(Exception e) {
                    // unexpectedly not valid
                    body = bodyAsString.substring(0, maxSize);
                    wellformed = false;
                }
            } else {
                body = bodyAsString;
                wellformed = true;
            }
        } else{
            // do not trust data from others to be wellformed
            if(bodyAsString.length() > maxSize) {
                try {
                    body = maxSizeJsonFilter.transform(bodyAsString);
                    wellformed = true;
                } catch(Exception e) {
                    // unexpectedly not valid
                    body = bodyAsString.substring(0, maxSize);
                    wellformed = false;
                }
            } else {
                body = bodyAsString;
                boolean verify = remoteHttpMessageContextSupplier.verifyJsonSyntax(request);
                if(verify) {
                    wellformed = jsonValidator.isWellformedJson(bodyAsString);
                } else {
                    wellformed = true;
                }
            }
        }
        return newRequestSingleFieldAppendingMarker(request, body, wellformed);
    }

    protected abstract Marker newRequestSingleFieldAppendingMarker(HttpRequest request, String body, boolean wellformed);

    public Marker createResponseMarker(Correlation correlation, HttpResponse response) {
        if(!ContentType.isJsonMediaType(response.getContentType())) {
            new ResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), null, false);
        }

        String bodyAsString;
        try {
            bodyAsString = response.getBodyAsString();
        } catch(Exception e){
            return newResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), null, false);
        }

        if(bodyAsString == null || bodyAsString.length() == 0) {
            return newResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), null, false);
        }

        String body = null;
        boolean wellformed;

        if (response.getOrigin().equals("local")) {
            // trust data from ourselves to be wellformed
            if(bodyAsString.length() > maxSize) {
                try {
                    body = maxSizeJsonFilter.transform(bodyAsString);
                    wellformed = true;
                } catch(Exception e) {
                    // unexpectedly not valid
                    body = bodyAsString.substring(0, maxSize);
                    wellformed = false;
                }
            } else {
                body = bodyAsString;
                wellformed = true;
            }
        } else{
            // do not trust data from others to be wellformed
            if(bodyAsString.length() > maxSize) {
                try {
                    body = maxSizeJsonFilter.transform(bodyAsString);
                    wellformed = true;
                } catch(Exception e) {
                    // unexpectedly not valid
                    body = bodyAsString.substring(0, maxSize);
                    wellformed = false;
                }
            } else {
                body = bodyAsString;
                boolean verify = remoteHttpMessageContextSupplier.verifyJsonSyntax(response);
                if(verify) {
                    wellformed = jsonValidator.isWellformedJson(bodyAsString);
                } else {
                    wellformed = true;
                }
            }
        }
        return newResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), body, wellformed);
    }

    protected abstract Marker newResponseSingleFieldAppendingMarker(HttpResponse response, long millis, String body, boolean wellformed);
}
