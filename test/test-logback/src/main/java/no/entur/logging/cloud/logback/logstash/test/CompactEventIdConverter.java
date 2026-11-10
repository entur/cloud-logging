package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Prints the id of the event (<code> #1a2b3c4d5e6f</code>, see {@link AiEventId}) if the compact file loses information: a stack trace,
 * or a message, request-response body or structured fields which are cut. The full file has the id of every event, so that the complete
 * event can be found there.
 */
public class CompactEventIdConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return isTruncatedInCompactFile(event) ? " #" + AiEventId.idOf(event) : "";
    }

    private static boolean isTruncatedInCompactFile(ILoggingEvent event) {
        if (event.getThrowableProxy() != null) {
            return true;
        }
        AiEventDetails details = AiEventDetails.of(event);
        if (details.isRequestResponse() && CompactRequestResponse.isBodyTruncatedInCompactFile(details)) {
            return true;
        }
        return CompactFieldsConverter.isTruncatedInCompactFile(event) || CompactMessageConverter.isTruncatedInCompactFile(event);
    }
}
