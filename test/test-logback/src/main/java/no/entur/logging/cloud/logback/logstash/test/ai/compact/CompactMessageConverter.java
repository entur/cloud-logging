package no.entur.logging.cloud.logback.logstash.test.ai.compact;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiAliases;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiEventDetails;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiText;

/**
 * Message for the compact file, on a single line with UUIDs aliased and cut (see {@link CompactSettings#AI_MESSAGE_MAX_PROPERTY}).
 * Request-response events get the headers and body from the request-response marker, with fewer headers and the body cut
 * (see {@link CompactRequestResponse}).
 */
public class CompactMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return "";
        }
        AiEventDetails details = AiEventDetails.of(event);
        if (details.isRequestResponse()) {
            return CompactRequestResponse.render(message, details, event.getMDCPropertyMap());
        }
        return CompactSettings.truncateMessage(compactMessage(message));
    }

    private static String compactMessage(String message) {
        return AiAliases.aliasUuids(AiText.singleLine(message));
    }

    /** True if the compact file cuts the message of the event (not of a request-response event, whose body is cut instead). */
    public static boolean isTruncatedInCompactFile(ILoggingEvent event) {
        int max = CompactSettings.messageMax();
        String message = event.getFormattedMessage();
        // escaping a control character makes it 6 characters, so shorter messages are never cut
        if (max <= 0 || message == null || message.length() <= max / 6 || AiEventDetails.of(event).isRequestResponse()) {
            return false;
        }
        return compactMessage(message).length() > max;
    }
}
