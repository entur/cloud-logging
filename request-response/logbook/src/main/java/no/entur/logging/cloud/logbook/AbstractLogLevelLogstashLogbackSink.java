package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.util.JsonValidator;
import no.entur.logging.cloud.logbook.util.MaxSizeJsonFilter;
import org.slf4j.Marker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

import javax.annotation.Nullable;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelLogstashLogbackSink extends AbstractLogLevelSink {

    protected final MaxSizeJsonFilter maxSizeJsonFilter;
    protected final JsonValidator jsonValidator;

    protected final int maxSize;

    protected final RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier;

    public AbstractLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled,
            JsonFactory jsonFactory, int maxSize, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier,
            MessageComposer server, MessageComposer client) {
        super(logLevelEnabled, logConsumer, server, client);

        this.maxSize = maxSize;
        this.maxSizeJsonFilter = new MaxSizeJsonFilter(maxSize, jsonFactory);
        this.jsonValidator = new JsonValidator(jsonFactory);

        this.remoteHttpMessageContextSupplier = remoteHttpMessageContextSupplier;
    }

    public Marker createRequestMarker(HttpRequest request) {

        String contentType = request.getContentType();
        boolean isJson = ContentType.isJsonMediaType(contentType);
        boolean isXml = isXmlMediaType(contentType);

        if (!isJson && !isXml) {
            return newRequestSingleFieldAppendingMarker(request, null, false);
        }

        String bodyAsString;
        try {
            bodyAsString = request.getBodyAsString();
        } catch (Exception e) {
            return newRequestSingleFieldAppendingMarker(request, null, false);
        }

        if (bodyAsString == null || bodyAsString.length() == 0) {
            return newRequestSingleFieldAppendingMarker(request, null, false);
        }

        // add sanity check for JSON content, even if mimetype does match
        if (!isJson || !smellsLikeJson(bodyAsString)) {
            if( bodyAsString.length() > maxSize) {
                // TODO add filter
                String truncatedBody = bodyAsString.substring(0, maxSize);
                return newRequestSingleFieldAppendingMarker(request, truncatedBody, false);
            }
            return newRequestSingleFieldAppendingMarker(request, bodyAsString, false);
        }

        String body;
        boolean wellformed;

        if (request.getOrigin() == Origin.LOCAL) {
            // trust data from ourselves to be wellformed
            if (bodyAsString.length() > maxSize) {
                try {
                    body = maxSizeJsonFilter.transform(bodyAsString);
                    wellformed = true;
                } catch (Exception e) {
                    // unexpectedly not valid
                    body = bodyAsString.substring(0, maxSize);
                    wellformed = false;
                }
            } else {
                body = bodyAsString;
                wellformed = true;
            }
        } else {
            // do not trust data from others to be wellformed
            if (bodyAsString.length() > maxSize) {
                try {
                    body = maxSizeJsonFilter.transform(bodyAsString);
                    wellformed = true;
                } catch (Exception e) {
                    // unexpectedly not valid
                    body = bodyAsString.substring(0, maxSize);
                    wellformed = false;
                }
            } else {
                body = bodyAsString;
                boolean verify = remoteHttpMessageContextSupplier.verifyJsonSyntax(request);
                if (verify) {
                    wellformed = jsonValidator.isWellformedJson(bodyAsString);
                } else {
                    wellformed = true;
                }
            }
        }
        return newRequestSingleFieldAppendingMarker(request, body, wellformed);
    }

    protected abstract Marker newRequestSingleFieldAppendingMarker(HttpRequest request, String body,
            boolean wellformed);

    public Marker createResponseMarker(Correlation correlation, HttpResponse response) {

        String contentType = response.getContentType();
        boolean isJson = ContentType.isJsonMediaType(contentType);
        boolean isXml = isXmlMediaType(contentType);

        if (!isJson && !isXml) {
            return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(), null, false);
        }

        String bodyAsString;
        try {
            bodyAsString = response.getBodyAsString();
        } catch (Exception e) {
            return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(), null, false);
        }

        if (bodyAsString == null || bodyAsString.length() == 0) {
            return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(), null, false);
        }

        // add sanity check for JSON content, even if mimetype does match
        if (!isJson || !smellsLikeJson(bodyAsString)) {
            if(bodyAsString.length() > maxSize) {
                // TODO add filter
                String truncatedBody = bodyAsString.substring(0, maxSize);
                return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(),
                        truncatedBody, false);
            }
            return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration(), bodyAsString,
                    false);
        }

        String body;
        boolean wellformed;

        if (response.getOrigin() == Origin.LOCAL) {
            // trust data from ourselves to be wellformed
            if (bodyAsString.length() > maxSize) {
                try {
                    body = maxSizeJsonFilter.transform(bodyAsString);
                    wellformed = true;
                } catch (Exception e) {
                    // unexpectedly not valid
                    body = bodyAsString.substring(0, maxSize);
                    wellformed = false;
                }
            } else {
                body = bodyAsString;
                wellformed = true;
            }
        } else {
            // do not trust data from others to be wellformed
            if (bodyAsString.length() > maxSize) {
                try {
                    body = maxSizeJsonFilter.transform(bodyAsString);
                    wellformed = true;
                } catch (Exception e) {
                    // unexpectedly not valid
                    body = bodyAsString.substring(0, maxSize);
                    wellformed = false;
                }
            } else {
                body = bodyAsString;
                boolean verify = remoteHttpMessageContextSupplier.verifyJsonSyntax(response);
                if (verify) {
                    wellformed = jsonValidator.isWellformedJson(bodyAsString);
                } else {
                    wellformed = true;
                }
            }
        }
        return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(), body, wellformed);
    }

    protected abstract Marker newResponseSingleFieldAppendingMarker(HttpResponse response, Duration duration, String body,
                                                                    boolean wellformed);
    public static boolean smellsLikeJson(String body) {
        if(body == null) {
            return false;
        }
        if(body.length() <= 1) {
            return false;
        }

        char start = body.charAt(0);
        char end = body.charAt(body.length() - 1);

        if (start != '{' && start != '[') {
            return false;
        }
        if (end != '}' && start != ']') {
            return false;
        }
        return true;
    }

}
