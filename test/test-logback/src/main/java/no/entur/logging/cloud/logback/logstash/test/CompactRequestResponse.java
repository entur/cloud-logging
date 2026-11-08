package no.entur.logging.cloud.logback.logstash.test;

import org.slf4j.MDC;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds a single-line request-response message for AI mode, <code>message | header: value; ... | body: &lt;body&gt;</code>, in two variants:
 * the full message (all headers, whole body) for <code>&lt;file&gt;.full</code>, and a compact message for the compact file, without the excluded
 * headers (see {@link AiLoggingControl#AI_EXCLUDED_HEADERS_PROPERTY}), without headers whose value is already in the MDC and with the body truncated.
 * Log the full message with {@link #getMarker()}, which carries the compact message.
 */
public class CompactRequestResponse {

    /** Transport and boilerplate HTTP headers. Content-type is kept as it often explains a failing request. */
    public static final Set<String> DEFAULT_EXCLUDED_HTTP_HEADERS = Set.of(
            "accept", "accept-encoding", "accept-language", "cache-control", "connection", "content-length", "date", "expires",
            "host", "http2-settings", "keep-alive", "pragma", "strict-transport-security", "transfer-encoding", "upgrade",
            "user-agent", "vary", "x-content-type-options", "x-frame-options", "x-xss-protection");

    /** Transport and boilerplate gRPC headers (content-type is always application/grpc). */
    public static final Set<String> DEFAULT_EXCLUDED_GRPC_HEADERS = Set.of(
            "content-type", "grpc-accept-encoding", "grpc-encoding", "te", "user-agent");

    private static final Pattern LINE_BREAKS = Pattern.compile("\\s*\\R\\s*");

    private final StringBuilder full;
    private final StringBuilder compact;
    private final Set<String> excludedHeaders;
    private final Collection<String> mdcValues;

    private int fullHeaders;
    private int compactHeaders;
    private boolean truncated;

    public CompactRequestResponse(CharSequence message, Set<String> defaultExcludedHeaders) {
        String line = LINE_BREAKS.matcher(message.toString().strip()).replaceAll(" ");
        this.full = new StringBuilder(256).append(line);
        this.compact = new StringBuilder(256).append(line);
        this.excludedHeaders = AiLoggingControl.excludedHeaders(defaultExcludedHeaders);
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        this.mdcValues = mdc == null ? Collections.emptySet() : Set.copyOf(mdc.values());
    }

    public CompactRequestResponse header(String name, String value) {
        String lowerCaseName = name.toLowerCase(Locale.ROOT);
        append(full, fullHeaders++, lowerCaseName, value);
        if (!excludedHeaders.contains(lowerCaseName) && (value == null || value.isEmpty() || !mdcValues.contains(value))) {
            append(compact, compactHeaders++, lowerCaseName, value);
        }
        return this;
    }

    private static void append(StringBuilder builder, int index, String name, String value) {
        builder.append(index == 0 ? " | " : "; ").append(name).append(": ").append(value);
    }

    /** Appends the body (expected to be single-line already); blank bodies are left out. */
    public CompactRequestResponse body(String body) {
        if (body != null && !body.isBlank()) {
            full.append(AiLoggingControl.BODY_MARKER).append(body);
            String truncatedBody = AiLoggingControl.truncateBody(body);
            truncated = truncatedBody.length() != body.length();
            compact.append(AiLoggingControl.BODY_MARKER).append(truncatedBody);
        }
        return this;
    }

    /** The full message, for <code>&lt;file&gt;.full</code> and other appenders. */
    public String getMessage() {
        return full.toString();
    }

    public String getCompactMessage() {
        return compact.toString();
    }

    public CompactMessageMarker getMarker() {
        return new CompactMessageMarker(getCompactMessage(), truncated);
    }
}
