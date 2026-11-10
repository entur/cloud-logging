package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Objects;

/**
 * A short id derived from an event (<code>1a2b3c4d5e6f</code>), so that the same event has the same id in every log file and the
 * compact file can point to the complete event in the full file.
 */
public final class AiEventId {

    private AiEventId() {
    }

    /**
     * The id: 48 bits (12 hex digits) of a 64-bit hash. An id is looked up when the compact file shows it, and the lookup is ambiguous
     * only if another event in the full file has the same id: with n events in the full file and k ids in the compact file, about
     * k * n / 2^48 such ids per run (0.00001 for a million events with 2,500 ids). If an id matches more than one event, the right one has
     * the same logger and message. Not aliased as a hex id: those are only aliased in request-response headers.
     */
    public static String idOf(ILoggingEvent event) {
        long hash = event.getTimeStamp();
        hash = hash * 1_000_003L + event.getNanoseconds();
        hash = hash * 31 + Objects.hashCode(event.getThreadName());
        hash = hash * 31 + Objects.hashCode(event.getLoggerName());
        hash = hash * 31 + (event.getLevel() == null ? 0 : event.getLevel().toInt());
        hash = hash * 31 + Objects.hashCode(event.getFormattedMessage());
        return hex12(mix(hash) & 0xFFFF_FFFF_FFFFL);
    }

    /** 12 hex digits with leading zeros (String.format parses its format on every call). */
    static String hex12(long value) {
        String hex = Long.toHexString(value);
        return hex.length() >= 12 ? hex : "000000000000".substring(hex.length()) + hex;
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
