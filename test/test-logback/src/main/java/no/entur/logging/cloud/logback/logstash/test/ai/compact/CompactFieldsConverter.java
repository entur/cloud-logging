package no.entur.logging.cloud.logback.logstash.test.ai.compact;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.ai.common.AiEventDetails;

/**
 * Structured fields for the compact file (see {@link AiEventDetails#FIELDS_PREFIX}), with UUIDs aliased and the JSON cut like
 * request-response bodies (see {@link CompactSettings#AI_BODY_MAX_PROPERTY}). Empty if there are none.
 */
public class CompactFieldsConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String fields = AiEventDetails.of(event).getCompactFields();
        return fields == null ? "" : AiEventDetails.FIELDS_PREFIX + CompactSettings.truncateBody(fields);
    }

    /** True if the compact file cuts the structured fields of the event. */
    public static boolean isTruncatedInCompactFile(ILoggingEvent event) {
        String fields = AiEventDetails.of(event).getCompactFields();
        return fields != null && !CompactSettings.truncateBody(fields).equals(fields);
    }
}
