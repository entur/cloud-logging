package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Objects;

/**
 * Prints a short id (<code> #1a2b3c4d</code>) derived from the event, so the same event has the same id in every log file.
 * By default only for events where the compact file loses information (a stack trace, or a request-response body or structured
 * fields which are truncated); with option <code>always</code> for all events (use in the full file).
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
        return " #" + idOf(event);
    }

    private static boolean isTruncatedInCompactFile(ILoggingEvent event) {
        if (event.getThrowableProxy() != null) {
            return true;
        }
        CompactMessageMarker marker = CompactMessageMarker.find(event);
        if (marker != null) {
            return marker.isTruncated();
        }
        return StructuredFieldsConverter.isTruncatedInCompactFile(event);
    }

    /** The id without the leading <code>" #"</code>: 32 bits of a 64-bit hash, so that ids are practically unique within a test run. */
    public static String idOf(ILoggingEvent event) {
        long hash = event.getTimeStamp();
        hash = hash * 1_000_003L + event.getNanoseconds();
        hash = hash * 31 + Objects.hashCode(event.getThreadName());
        hash = hash * 31 + Objects.hashCode(event.getLoggerName());
        hash = hash * 31 + (event.getLevel() == null ? 0 : event.getLevel().toInt());
        hash = hash * 31 + Objects.hashCode(event.getFormattedMessage());
        return String.format("%08x", mix(hash) & 0xFFFFFFFFL);
    }

    /** MurmurHash3 finalizer: spreads every input bit over the output, so that the low 32 bits are well distributed. */
    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
