package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logger name for the compact file: the simple class name if the logger is named after a class (last segment starts with an upper-case
 * letter, e.g. <code>DispatcherServlet</code> or <code>Outer$Inner</code>), otherwise the full name (e.g. <code>no.entur.logging.cloud</code>).
 * The full file keeps the full name.
 */
public class CompactLoggerConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return shorten(event.getLoggerName());
    }

    static String shorten(String name) {
        if (name == null) {
            return "";
        }
        int index = name.lastIndexOf('.');
        if (index == -1 || index == name.length() - 1 || !Character.isUpperCase(name.charAt(index + 1))) {
            return name;
        }
        return name.substring(index + 1);
    }
}
