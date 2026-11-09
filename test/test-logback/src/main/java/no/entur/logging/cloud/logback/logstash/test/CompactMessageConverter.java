package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Message for the compact file, on a single line with UUIDs aliased. Request-response events get the headers and body from the
 * request-response marker, with fewer headers and the body cut (see {@link CompactRequestResponse}). Other files keep the full message.
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
            return CompactRequestResponse.render(message, details, event.getMDCPropertyMap(), true);
        }
        return MdcAliasConverter.aliasUuids(AiText.singleLine(message));
    }
}
