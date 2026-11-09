package no.entur.logging.cloud.logback.logstash.test;

import org.slf4j.MDC;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

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
        // not Set.copyOf: MDC values may be null
        this.mdcValues = mdc == null ? Collections.emptySet() : new HashSet<>(mdc.values());
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
            truncated = !truncatedBody.equals(body);
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

    /**
     * The body on a single line. Valid JSON (if <code>json</code>) only loses the whitespace between tokens, so the content (duplicate
     * fields, number formats, escapes) is unchanged; anything else has line breaks and the whitespace around them replaced by a single space.
     */
    public static String singleLineBody(String body, boolean json) {
        String stripped = body.strip();
        if (json && isValidJson(stripped)) {
            return removeWhitespaceBetweenTokens(stripped);
        }
        return LINE_BREAKS.matcher(stripped).replaceAll(" ");
    }

    private static boolean isValidJson(String body) {
        try (JsonParser parser = JSON_FACTORY.createParser(ObjectReadContext.empty(), body)) {
            while (parser.nextToken() != null) {
                // parse to the end
            }
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }

    private static String removeWhitespaceBetweenTokens(String json) {
        StringBuilder builder = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (Character.isWhitespace(c)) {
                continue;
            }
            builder.append(c);
        }
        return builder.toString();
    }
}
