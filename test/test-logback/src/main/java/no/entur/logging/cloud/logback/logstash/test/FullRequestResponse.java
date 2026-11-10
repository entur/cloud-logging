package no.entur.logging.cloud.logback.logstash.test;

import java.util.Locale;

/**
 * Renders request-response events (HTTP and gRPC) for the full file (see {@link AiRequestResponse}): all headers and the whole body;
 * aliases are followed by their value the first time.
 */
public final class FullRequestResponse {

    private FullRequestResponse() {
    }

    /** The message with all headers and the whole body. */
    public static String render(String message, AiEventDetails details) {
        // sized for the body (not aliased yet: aliases are announced in the order they appear, message and headers first)
        String body = details.getSingleLineBody();
        StringBuilder builder = AiRequestResponse.message(message, details, true, body == null ? 0 : body.length());
        int count = 0;
        for (AiEventDetails.Header header : details.getHeaders()) {
            AiRequestResponse.appendHeader(builder, count++, header.name().toLowerCase(Locale.ROOT), header.value(), true);
        }
        return AiRequestResponse.withBody(builder, body == null ? null : AiAliases.aliasUuids(body, true));
    }
}
