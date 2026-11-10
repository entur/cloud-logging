package no.entur.logging.cloud.logback.logstash.test;

/**
 * The parts of a request-response event (HTTP and gRPC) in the AI files: the event's message (unchanged, as without AI mode) followed
 * by the headers and body from the request-response marker, <code>message | header: value; ... | body: &lt;body&gt;</code>, on a single
 * line. UUIDs are aliased in the message, headers and body, hex ids in headers (for example in <code>traceparent</code>). With
 * <code>full</code>, an alias is followed by its value the first time, so the parts are aliased in the order they are written.
 */
public final class AiRequestResponse {

    /** Separates the body from the rest of a request-response message. */
    public static final String BODY_MARKER = " | body: ";

    // added by the sink's message composer if the body in the JSON output was cut: ' [truncated ~123 bytes]' or ' [truncated ~4KB]'
    private static final String TRUNCATED_NOTE = "[truncated ~";

    private AiRequestResponse() {
    }

    /**
     * A builder with room for the body, holding the message on a single line with UUIDs aliased. If the AI files have the whole body,
     * the note of the sink's message composer about the JSON output's body is left out, as it would contradict it.
     */
    public static StringBuilder message(String message, AiEventDetails details, boolean full, int bodyCapacity) {
        String text = message.strip();
        if (details.isCompleteBody()) {
            text = withoutTruncatedNote(text);
        }
        return new StringBuilder(256 + bodyCapacity).append(AiAliases.aliasUuids(AiText.singleLine(text), full));
    }

    /** Appends a header (<code>index</code> 0 for the first one) on a single line, with UUIDs and hex ids aliased. */
    public static void appendHeader(StringBuilder builder, int index, String name, String value, boolean full) {
        builder.append(index == 0 ? " | " : "; ").append(name).append(": ").append(AiAliases.aliasIds(AiText.singleLine(value), full));
    }

    /** The message with the body appended, if there is one. */
    public static String withBody(StringBuilder builder, String body) {
        if (body != null) {
            builder.append(BODY_MARKER).append(body);
        }
        return builder.toString();
    }

    /** The text without a trailing truncation note of the sink's message composer (and the whitespace before it). */
    static String withoutTruncatedNote(String text) {
        if (!text.endsWith("]")) {
            return text;
        }
        int start = text.lastIndexOf(TRUNCATED_NOTE);
        if (start == -1) {
            return text;
        }
        int digits = start + TRUNCATED_NOTE.length();
        int end = digits;
        while (end < text.length() && text.charAt(end) >= '0' && text.charAt(end) <= '9') {
            end++;
        }
        if (end == digits || !(text.startsWith(" bytes]", end) && end + 7 == text.length() || text.startsWith("KB]", end) && end + 3 == text.length())) {
            return text;
        }
        int before = start;
        while (before > 0 && Character.isWhitespace(text.charAt(before - 1))) {
            before--;
        }
        return text.substring(0, before);
    }
}
