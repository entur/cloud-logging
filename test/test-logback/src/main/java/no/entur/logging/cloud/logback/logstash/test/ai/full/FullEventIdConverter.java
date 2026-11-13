package no.entur.logging.cloud.logback.logstash.test.ai.full;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiEventId;

/**
 * Prints the id of every event (<code> #1a2b3c4d5e6f</code>, see {@link AiEventId}), so that an event whose id the compact file shows
 * can be found in the full file.
 */
public class FullEventIdConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return " #" + AiEventId.idOf(event);
    }
}
