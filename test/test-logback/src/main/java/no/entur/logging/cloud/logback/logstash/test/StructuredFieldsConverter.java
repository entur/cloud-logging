package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Prints the structured fields of an event which are not part of its message, <code> | fields: {"key":value, ...}</code>, as in the
 * JSON output: the fields of Logstash markers (for example <code>Markers.append("system", map)</code>), SLF4J key-value pairs and
 * structured arguments without a <code>{}</code> placeholder (see {@link AiEventDetails}). Request-response headers and body are left
 * out (the message has them); the full file gets the other request-response details (see {@link AiEventDetails#getFullFields()}).
 * Empty if there are none. UUIDs are aliased.
 * With option <code>compact</code> (for the compact file), the JSON is cut like request-response bodies
 * (see {@link AiLoggingControl#AI_BODY_MAX_PROPERTY}).
 */
public class StructuredFieldsConverter extends ClassicConverter {

    public static final String PREFIX = " | fields: ";

    private boolean compact;

    @Override
    public void start() {
        compact = "compact".equals(getFirstOption());
        super.start();
    }

    @Override
    public String convert(ILoggingEvent event) {
        AiEventDetails details = AiEventDetails.of(event);
        if (compact) {
            String fields = details.getCompactFields();
            return fields == null ? "" : PREFIX + AiLoggingControl.truncateBody(fields);
        }
        String fields = details.getFullFields();
        return fields == null ? "" : PREFIX + MdcAliasConverter.aliasUuids(fields, true);
    }

    /** True if the compact file cuts the structured fields of the event. */
    public static boolean isTruncatedInCompactFile(ILoggingEvent event) {
        String fields = AiEventDetails.of(event).getCompactFields();
        return fields != null && !AiLoggingControl.truncateBody(fields).equals(fields);
    }
}
