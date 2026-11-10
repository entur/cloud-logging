package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Structured fields for the full file (see {@link AiEventDetails#FIELDS_PREFIX}), not cut, and with the other details of a
 * request-response event (see {@link AiEventDetails#getFullFields()}). UUIDs are aliased, and the first time an alias is written it is
 * followed by its value. Empty if there are none.
 */
public class FullFieldsConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String fields = AiEventDetails.of(event).getFullFields();
        return fields == null ? "" : AiEventDetails.FIELDS_PREFIX + AiAliases.aliasUuids(fields, true);
    }
}
