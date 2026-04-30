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

    public RequestResponseSingleFieldAppendingMarker createRequestMarker(HttpRequest request) {

        String contentType = request.getContentType();
        boolean isJson = ContentType.isJsonMediaType(contentType);
        boolean isXml = isXmlMediaType(contentType);

        if (!isJson && !isXml) {
            return newRequestSingleFieldAppendingMarker(request, null, false, -1);
        }

        String bodyAsString;
        try {
            bodyAsString = request.getBodyAsString();
        } catch (Exception e) {
            return newRequestSingleFieldAppendingMarker(request, null, false, -1);
        }

        if (bodyAsString == null || bodyAsString.length() == 0) {
            return newRequestSingleFieldAppendingMarker(request, null, false, -1);
        }

        // add sanity check for JSON content, even if mimetype does match
        if (!isJson || !smellsLikeJson(bodyAsString)) {
            if (bodyAsString.length() > maxSize) {
                // TODO add filter
                int truncated = bodyAsString.length() - maxSize;
                String truncatedBody = bodyAsString.substring(0, maxSize);
                return newRequestSingleFieldAppendingMarker(request, truncatedBody, false, truncated);
            }
            return newRequestSingleFieldAppendingMarker(request, bodyAsString, false, -1);
        }

        String body;
        boolean wellformed;

        int truncated = -1;
        if (request.getOrigin() == Origin.LOCAL) {
            // trust data from ourselves to be wellformed and not pretty-printed
            if (bodyAsString.length() > maxSize) {
                truncated = bodyAsString.length() - maxSize;
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
                truncated = bodyAsString.length() - maxSize;
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
        return newRequestSingleFieldAppendingMarker(request, body, wellformed, truncated);
    }

    protected abstract RequestResponseSingleFieldAppendingMarker newRequestSingleFieldAppendingMarker(HttpRequest request, String body,
                                                                                                      boolean wellformed, int truncated);

    public RequestResponseSingleFieldAppendingMarker createResponseMarker(Correlation correlation, HttpResponse response) {

        String contentType = response.getContentType();
        boolean isJson = ContentType.isJsonMediaType(contentType);
        boolean isXml = isXmlMediaType(contentType);

        if (!isJson && !isXml) {
            return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(), null, false, -1);
        }

        String bodyAsString;
        try {
            bodyAsString = response.getBodyAsString();
        } catch (Exception e) {
            return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(), null, false, -1);
        }

        if (bodyAsString == null || bodyAsString.length() == 0) {
            return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(), null, false, -1);
        }

        // add sanity check for JSON content, even if mimetype does match
        if (!isJson || !smellsLikeJson(bodyAsString)) {
            if(bodyAsString.length() > maxSize) {
                // TODO add filter
                int truncated = bodyAsString.length() - maxSize;

                String truncatedBody = bodyAsString.substring(0, maxSize);
                return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(),
                        truncatedBody, false, truncated);
            }
            return new ResponseSingleFieldAppendingMarker(response, correlation.getDuration(), bodyAsString,
                    false, -1);
        }

        String body;
        boolean wellformed;
        int truncated = -1;

        if (response.getOrigin() == Origin.LOCAL) {
            // trust data from ourselves to be wellformed
            if (bodyAsString.length() > maxSize) {
                truncated = bodyAsString.length() - maxSize;
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
                truncated = bodyAsString.length() - maxSize;
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
        return newResponseSingleFieldAppendingMarker(response, correlation.getDuration(), body, wellformed, truncated);
    }

    protected abstract RequestResponseSingleFieldAppendingMarker newResponseSingleFieldAppendingMarker(HttpResponse response, Duration duration, String body,
                                                                                                       boolean wellformed, int truncated);

    /**
     *
     * We only check for arrays and objects, while technically more values can be valid JSON.
     *
     * @param body input
     * @return true if array or object start + end
     */

    public static boolean smellsLikeJson(String body) {
        if(body == null) {
            return false;
        }
        if(body.length() <= 1) {
            return false;
        }

        int start = findFirstNonWhitespaceCharacter(body);
        int end = findLastNonWhitespaceCharacter(body);

        if (start == '{' && end == '}') {
            return true;
        }

        if (start == '[' && end == ']') {
            return true;
        }

        return false;
    }

    private static int findFirstNonWhitespaceCharacter(String body) {
        for(int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }

        return -1;
    }

    private static int findLastNonWhitespaceCharacter(String body) {
        for(int i = body.length() - 1; i >= 0; i--) {
            char c = body.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }

        return -1;
    }

}
