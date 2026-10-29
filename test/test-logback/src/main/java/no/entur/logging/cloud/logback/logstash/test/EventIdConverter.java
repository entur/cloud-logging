package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Objects;

/**
 * Prints a short id (<code> #a1b2c3</code>) derived from the event, so the same event has the same id in every log file.
 * By default only for events where the compact file loses information (a stack trace, or a request-response body which is
 * truncated); with option <code>always</code> for all events (use in the full file).
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
        String message = event.getFormattedMessage();
        if (!always && event.getThrowableProxy() == null && !isBodyTruncated(message)) {
            return "";
        }
        long hash = Objects.hash(event.getTimeStamp(), event.getNanoseconds(), event.getLoggerName(), event.getLevel(), message);
        return " #" + String.format("%06x", hash & 0xFFFFFFL);
    }

    private static boolean isBodyTruncated(String message) {
        if (message == null) {
            return false;
        }
        int index = message.indexOf(AiLoggingControl.BODY_MARKER);
        return index != -1 && !AiLoggingControl.truncateBody(message.substring(index + AiLoggingControl.BODY_MARKER.length())).equals(message.substring(index + AiLoggingControl.BODY_MARKER.length()));
    }
}
