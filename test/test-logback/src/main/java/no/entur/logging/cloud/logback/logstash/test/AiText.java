package no.entur.logging.cloud.logback.logstash.test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

/**
 * Text helpers for the AI files: every event on a single line, control characters escaped (a NUL byte makes grep treat the
 * whole file as binary), binary bodies replaced by a short note.
 */
public final class AiText {


    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private AiText() {
    }

    /** Line breaks (and the whitespace around them) replaced by a single space, other control characters escaped. */
    public static String singleLine(String text) {
        if (text == null) {
            return "";
        }
        if (hasLineBreak(text)) {
            text = joinLines(text);
        }
        return escapeControlCharacters(text, false);
    }

    private static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r' || c == '\u000B' || c == '\f' || c == '\u0085' || c == '\u2028' || c == '\u2029';
    }

    private static boolean isSpaceOrLineBreak(char c) {
        return c == ' ' || c == '\t' || isLineBreak(c);
    }

    private static boolean hasLineBreak(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (isLineBreak(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Every run of spaces, tabs and line breaks which contains a line break becomes a single space; one pass, no regular expression. */
    private static String joinLines(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (!isSpaceOrLineBreak(c)) {
                builder.append(c);
                i++;
                continue;
            }
            int end = i;
            boolean lineBreak = false;
            while (end < text.length() && isSpaceOrLineBreak(text.charAt(end))) {
                lineBreak |= isLineBreak(text.charAt(end));
                end++;
            }
            if (lineBreak) {
                builder.append(' ');
            } else {
                builder.append(text, i, end);
            }
            i = end;
        }
        return builder.toString();
    }

    /**
     * The text cut to at most <code>max</code> characters, but not inside an alias (<code>u12</code> from <code>u1234</code> would be
     * another alias) or a surrogate pair. The text itself if it is not longer.
     */
    public static String cut(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        int end = max;
        if (Character.isDigit(text.charAt(end))) {
            int start = end;
            while (start > 0 && Character.isDigit(text.charAt(start - 1))) {
                start--;
            }
            if (start > 0 && (text.charAt(start - 1) == 'u' || text.charAt(start - 1) == 'h')
                    && (start == 1 || !Character.isLetterOrDigit(text.charAt(start - 2)))) {
                end = start - 1;
            }
        }
        if (end > 0 && Character.isLowSurrogate(text.charAt(end)) && Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    /** Control characters other than tab (and, if <code>keepLineBreaks</code>, line breaks) as <code>\\uXXXX</code>. */
    public static String escapeControlCharacters(String text, boolean keepLineBreaks) {
        int i = firstControlCharacter(text, keepLineBreaks);
        if (i == -1) {
            return text;
        }
        StringBuilder builder = new StringBuilder(text.length() + 16).append(text, 0, i);
        for (; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isEscaped(c, keepLineBreaks)) {
                builder.append(String.format("\\u%04x", (int) c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static int firstControlCharacter(String text, boolean keepLineBreaks) {
        for (int i = 0; i < text.length(); i++) {
            if (isEscaped(text.charAt(i), keepLineBreaks)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isEscaped(char c, boolean keepLineBreaks) {
        if (c == '\t' || (keepLineBreaks && (c == '\n' || c == '\r'))) {
            return false;
        }
        return c < 0x20 || c == 0x7f;
    }

    /**
     * A body on a single line. Valid JSON (if <code>json</code>) only loses the whitespace between tokens, so the content (duplicate
     * fields, number formats, escapes) is unchanged; anything else has line breaks and the whitespace around them replaced by a single
     * space. Binary content (a NUL character, or more than 1 in 10 control characters) is replaced by <code>[binary, N chars]</code>.
     */
    public static String singleLineBody(String body, boolean json) {
        return singleLineBody(body, json, false);
    }

    /** Like {@link #singleLineBody(String, boolean)}; <code>validJson</code> if the body is known to be valid JSON (not parsed again). */
    static String singleLineBody(String body, boolean json, boolean validJson) {
        if (isBinary(body)) {
            return "[binary, " + body.length() + " chars]";
        }
        String stripped = body.strip();
        if (json && (validJson || isValidJson(stripped))) {
            return removeWhitespaceBetweenTokens(stripped);
        }
        return singleLine(stripped);
    }

    static boolean isBinary(String body) {
        int control = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == 0) {
                return true;
            }
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                control++;
            }
        }
        return control > 0 && control * 10 > body.length();
    }

    /** True if the text is exactly one JSON value (the parser would also accept several, such as <code>1 2</code>). */
    static boolean isValidJson(String body) {
        try (JsonParser parser = JSON_FACTORY.createParser(ObjectReadContext.empty(), body)) {
            if (parser.nextToken() == null) {
                return false;
            }
            parser.skipChildren();
            return parser.nextToken() == null;
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
