package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Message for the compact file: the compact variant from {@link CompactMessageMarker} if present (request-response logging,
 * with fewer headers and a truncated body), otherwise the message itself; line breaks collapsed and UUIDs aliased.
 * Other files keep the full message.
 */
public class CompactMessageConverter extends ClassicConverter {

    private static final Pattern LINE_BREAKS = Pattern.compile("\\s*[\\r\\n]+\\s*");

    @Override
    public String convert(ILoggingEvent event) {
        CompactMessageMarker marker = CompactMessageMarker.find(event);
        String message = marker != null ? marker.getCompactMessage() : event.getFormattedMessage();
        if (message == null) {
            return "";
        }
        return MdcAliasConverter.aliasUuids(singleLine(message));
    }

    /** Replaces line breaks (and surrounding whitespace) by a single space, so that every event is on one line. */
    public static String singleLine(String text) {
        if (text.indexOf('\n') == -1 && text.indexOf('\r') == -1) {
            return text;
        }
        return LINE_BREAKS.matcher(text).replaceAll(" ");
    }
}
