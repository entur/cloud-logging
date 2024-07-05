package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.util.JsonValidator;
import no.entur.logging.cloud.logbook.util.MaxSizeJsonFilter;
import org.slf4j.Marker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelLogstashLogbackSink extends AbstractLogLevelSink {

    public static boolean isXmlMediaType(@Nullable final String contentType) {
        if (contentType == null) {
            return false;
        }

        String contentTypeWithoutEncoding;
        // text/xml;charset=UTF-8
        int index = contentType.indexOf(';');
        if(index == -1) {
            contentTypeWithoutEncoding = contentType;
        } else {
            contentTypeWithoutEncoding = contentType.substring(0, index).trim();
        }

        final String lowerCasedContentType = contentTypeWithoutEncoding.toLowerCase();

        boolean isApplicationOrText = lowerCasedContentType.startsWith("application/") || lowerCasedContentType.startsWith("text/");
        if(!isApplicationOrText) {
            return false;
        }

        return lowerCasedContentType.endsWith("+xml") || lowerCasedContentType.endsWith("/xml");
    }

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

        String contentType = request.getContentType();
        boolean isJson = ContentType.isJsonMediaType(contentType);
        boolean isXml = isXmlMediaType(contentType);

        if(!isJson && !isXml) {
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

        if (!isJson) {
            if(bodyAsString.length() > maxSize) {
                // TODO add filter
                String truncatedBody = bodyAsString.substring(0, maxSize);
                return newRequestSingleFieldAppendingMarker(request, truncatedBody, false);
            }
            return newRequestSingleFieldAppendingMarker(request, bodyAsString, false);
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

        String contentType = response.getContentType();
        boolean isJson = ContentType.isJsonMediaType(contentType);
        boolean isXml = isXmlMediaType(contentType);

        if(!isJson && !isXml) {
            return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), null, false);
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

        if (!isJson) {
            if(bodyAsString.length() > maxSize) {
                // TODO add filter
                String truncatedBody = bodyAsString.substring(0, maxSize);
                return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), truncatedBody, false);
            }
            return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), bodyAsString, false);
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
