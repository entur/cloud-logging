package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Objects;

/**
 * Prints a short id (<code> #1a2b3c4d5e6f</code>) derived from the event, so the same event has the same id in every log file.
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
        return " #" + idOf(event);
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

    /**
     * The id without the leading <code>" #"</code>: 48 bits (12 hex digits) of a 64-bit hash. An id is looked up when the compact file
     * shows it, and the lookup is ambiguous only if another event in the full file has the same id: with n events in the full file and k
     * ids in the compact file, about k * n / 2^48 such ids per run (0.00001 for a million events with 2,500 ids). If an id matches more
     * than one event, the right one has the same logger and message. Not aliased as a hex id: those are only aliased in request-response
     * headers.
     */
    public static String idOf(ILoggingEvent event) {
        long hash = event.getTimeStamp();
        hash = hash * 1_000_003L + event.getNanoseconds();
        hash = hash * 31 + Objects.hashCode(event.getThreadName());
        hash = hash * 31 + Objects.hashCode(event.getLoggerName());
        hash = hash * 31 + (event.getLevel() == null ? 0 : event.getLevel().toInt());
        hash = hash * 31 + Objects.hashCode(event.getFormattedMessage());
        return String.format("%012x", mix(hash) & 0xFFFF_FFFF_FFFFL);
    }

    /** MurmurHash3 finalizer: spreads every input bit over the output, so that the low 48 bits are well distributed. */
    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
