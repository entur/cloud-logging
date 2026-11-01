package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/** Like <code>%msg</code>, but UUIDs are replaced by the same aliases as in the compact file. Use in the full file. */
public class AliasMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return MdcAliasConverter.aliasUuids(event.getFormattedMessage());
    }
}
