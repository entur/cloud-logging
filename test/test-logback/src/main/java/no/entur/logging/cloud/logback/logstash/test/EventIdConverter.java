package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Prints the id of the event (<code> #1a2b3c4d5e6f</code>, see {@link AiEventId}).
 * By default only for events where the compact file loses information (a stack trace, or a message, request-response body or
 * structured fields which are truncated); with option <code>always</code> for all events (use in the full file).
 */
public class EventIdConverter extends ClassicConverter {

    private boolean always;

    @Override
    public void start() {
        always = "always".equals(getFirstOption());
        super.start();
    }

    @Override
    public String convert(ILoggingEvent event) {
        if (!always && !isTruncatedInCompactFile(event)) {
            return "";
        }
        return " #" + AiEventId.idOf(event);
    }

    private static boolean isTruncatedInCompactFile(ILoggingEvent event) {
        if (event.getThrowableProxy() != null) {
            return true;
        }
        AiEventDetails details = AiEventDetails.of(event);
        if (details.isRequestResponse() && CompactRequestResponse.isBodyTruncatedInCompactFile(details)) {
            return true;
        }
        return StructuredFieldsConverter.isTruncatedInCompactFile(event) || CompactMessageConverter.isTruncatedInCompactFile(event);
    }
}
