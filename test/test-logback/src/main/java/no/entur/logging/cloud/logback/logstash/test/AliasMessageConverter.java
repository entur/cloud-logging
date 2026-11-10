package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Like <code>%msg</code>, but UUIDs are replaced by the same aliases as in the compact file (followed by the value the first time)
 * and control characters other than line breaks are escaped. Request-response events get all headers and the whole body from the
 * request-response marker (see {@link CompactRequestResponse}). Use in the full file.
 */
public class AliasMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return "";
        }
        AiEventDetails details = AiEventDetails.of(event);
        if (details.isRequestResponse()) {
            return CompactRequestResponse.render(message, details, event.getMDCPropertyMap(), false);
        }
        return AiAliases.aliasUuids(AiText.escapeControlCharacters(message, true), true);
    }
}
