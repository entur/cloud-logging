package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logback.logstash.test.AiBodyMarker;
import no.entur.logging.cloud.logbook.AbstractLogLevelSink;
import no.entur.logging.cloud.logbook.RequestResponseSingleFieldAppendingMarker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpMessage;

import java.io.IOException;
import java.util.Locale;

/**
 * AI mode: attaches the whole body of textual requests and responses to the request-response marker as an {@link AiBodyMarker},
 * which the AI files print and JSON output ignores.
 */
public final class AiBodies {

    private AiBodies() {
    }

    public static <M extends RequestResponseSingleFieldAppendingMarker> M attach(M marker, HttpMessage message) {
        if (marker == null) {
            return null;
        }
        String contentType = message.getContentType();
        if (!isTextMediaType(contentType)) {
            return marker;
        }
        String body = readBody(message);
        if (body != null && !body.isBlank()) {
            marker.add(new AiBodyMarker(body, ContentType.isJsonMediaType(contentType)));
        }
        return marker;
    }

    private static String readBody(HttpMessage message) {
        try {
            return message.getBodyAsString();
        } catch (IOException | RuntimeException e) {
            // logged without the body
            return null;
        }
    }

    /**
     * JSON, XML, <code>text/*</code> and form data; not binary content. A missing content type counts as text (binary content is still
     * recognized when the body is printed).
     */
    static boolean isTextMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        if (ContentType.isJsonMediaType(contentType) || AbstractLogLevelSink.isXmlMediaType(contentType)) {
            return true;
        }
        String lowerCase = contentType.toLowerCase(Locale.ROOT).strip();
        return lowerCase.startsWith("text/") || lowerCase.startsWith("application/x-www-form-urlencoded");
    }
}
