package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Message converter which truncates the request-response body (see {@link AiLoggingControl#BODY_MARKER}) to
 * {@link AiLoggingControl#AI_BODY_MAX_PROPERTY} characters, if set. Use for the compact file only, so that other files keep the full body.
 */
public class TruncateBodyMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return "";
        }
        int index = message.indexOf(AiLoggingControl.BODY_MARKER);
        if (index == -1) {
            return message;
        }
        int bodyStart = index + AiLoggingControl.BODY_MARKER.length();
        return message.substring(0, bodyStart) + AiLoggingControl.truncateBody(message.substring(bodyStart));
    }
}
