package no.entur.logging.cloud.logback.logstash.test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Renders request-response events (HTTP and gRPC) for the AI files: the event's message (unchanged, as without AI mode) followed by
 * the headers and body from the request-response marker, <code>message | header: value; ... | body: &lt;body&gt;</code>, on a single line.
 * <ul>
 *     <li>The full file gets all headers and the whole body; aliases are followed by their value the first time.</li>
 *     <li>The compact file leaves out transport headers (see {@link AiLoggingControl#AI_EXCLUDED_HEADERS_PROPERTY}) and headers
 *     repeated in the MDC (same name, ignoring an <code>x-</code> prefix, case and punctuation, and the same value), and cuts the body
 *     (see {@link AiLoggingControl#AI_BODY_MAX_PROPERTY}).</li>
 * </ul>
 * UUIDs are aliased in headers and bodies, hex ids in headers (for example in <code>traceparent</code>); aliasing comes before cutting.
 */
public final class CompactRequestResponse {

    /** Transport and boilerplate HTTP and gRPC headers. Content-type is kept (except for gRPC) as it often explains a failing request. */
    public static final Set<String> DEFAULT_EXCLUDED_HEADERS = Set.of(
            "accept", "accept-encoding", "accept-language", "cache-control", "connection", "content-length", "date", "expires",
            "host", "http2-settings", "keep-alive", "pragma", "strict-transport-security", "transfer-encoding", "upgrade",
            "user-agent", "vary", "x-content-type-options", "x-frame-options", "x-xss-protection",
            "grpc-accept-encoding", "grpc-encoding", "te");

    private CompactRequestResponse() {
    }

    /** The message with headers and body, for the full (<code>compact</code> false) or the compact file. */
    public static String render(String message, AiEventDetails details, Map<String, String> mdc, boolean compact) {
        StringBuilder builder = new StringBuilder(256).append(MdcAliasConverter.aliasUuids(AiText.singleLine(message.strip()), !compact));
        Set<String> excluded = compact ? AiLoggingControl.excludedHeaders(DEFAULT_EXCLUDED_HEADERS) : Set.of();
        Map<String, String> mdcByName = compact ? byNormalizedName(mdc) : Map.of();
        int count = 0;
        for (AiEventDetails.Header header : details.getHeaders()) {
            String name = header.name().toLowerCase(Locale.ROOT);
            String value = header.value();
            if (compact && (excluded.contains(name) || isGrpcContentType(name, value) || repeatsMdc(name, value, mdcByName))) {
                continue;
            }
            builder.append(count++ == 0 ? " | " : "; ").append(name).append(": ").append(MdcAliasConverter.aliasIds(AiText.singleLine(value), !compact));
        }
        String body = compact ? compactBody(details) : fullBody(details);
        if (body != null) {
            builder.append(AiLoggingControl.BODY_MARKER).append(body);
        }
        return builder.toString();
    }

    /** True if the compact file cuts the body of this event. */
    public static boolean isBodyTruncatedInCompactFile(AiEventDetails details) {
        String body = aliasedBody(details, false);
        return body != null && !AiLoggingControl.truncateBody(body).equals(body);
    }

    private static String fullBody(AiEventDetails details) {
        return aliasedBody(details, true);
    }

    private static String compactBody(AiEventDetails details) {
        String body = aliasedBody(details, false);
        return body == null ? null : AiLoggingControl.truncateBody(body);
    }

    private static String aliasedBody(AiEventDetails details, boolean full) {
        String body = details.getBody();
        if (body == null || body.isBlank()) {
            return null;
        }
        return MdcAliasConverter.aliasUuids(AiText.singleLineBody(body, details.isJsonBody()), full);
    }

    private static boolean isGrpcContentType(String name, String value) {
        return name.equals("content-type") && value != null && value.startsWith("application/grpc");
    }

    private static boolean repeatsMdc(String name, String value, Map<String, String> mdcByName) {
        return value != null && !value.isEmpty() && value.equals(mdcByName.get(normalizedName(name)));
    }

    private static Map<String, String> byNormalizedName(Map<String, String> mdc) {
        if (mdc == null || mdc.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : mdc.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(normalizedName(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    /** <code>X-Correlation-Id</code>, <code>correlationId</code> and <code>correlation_id</code> are all <code>correlationid</code>. */
    static String normalizedName(String name) {
        String lowerCase = name.toLowerCase(Locale.ROOT);
        int slash = lowerCase.lastIndexOf('/');
        if (slash != -1) {
            lowerCase = lowerCase.substring(slash + 1);
        }
        if (lowerCase.startsWith("x-")) {
            lowerCase = lowerCase.substring(2);
        }
        StringBuilder builder = new StringBuilder(lowerCase.length());
        for (int i = 0; i < lowerCase.length(); i++) {
            char c = lowerCase.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
