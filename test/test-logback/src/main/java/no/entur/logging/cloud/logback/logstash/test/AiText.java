package no.entur.logging.cloud.logback.logstash.test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

import java.util.regex.Pattern;

/**
 * Text helpers for the AI files: every event on a single line, control characters escaped (a NUL byte makes grep treat the
 * whole file as binary), binary bodies replaced by a short note.
 */
public final class AiText {

    private static final Pattern LINE_BREAKS = Pattern.compile("\\s*\\R\\s*");

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private AiText() {
    }

    /** Line breaks (and the whitespace around them) replaced by a single space, other control characters escaped. */
    public static String singleLine(String text) {
        if (text == null) {
            return "";
        }
        if (text.indexOf('\n') != -1 || text.indexOf('\r') != -1) {
            text = LINE_BREAKS.matcher(text).replaceAll(" ");
        }
        return escapeControlCharacters(text, false);
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
        if (isBinary(body)) {
            return "[binary, " + body.length() + " chars]";
        }
        String stripped = body.strip();
        if (json && isValidJson(stripped)) {
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

    static boolean isValidJson(String body) {
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
