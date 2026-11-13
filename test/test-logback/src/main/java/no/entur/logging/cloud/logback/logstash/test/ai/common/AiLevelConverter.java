package no.entur.logging.cloud.logback.logstash.test.ai.common;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.slf4j.Marker;

import java.util.Iterator;
import java.util.List;

/**
 * Like <code>%level</code>, with the error severity of {@link no.entur.logging.cloud.api.DevOpsLogger} if there is one:
 * <code>ERROR(TELL_ME_TOMORROW)</code>, <code>ERROR(INTERRUPT_MY_DINNER)</code> (CRITICAL in GCP) and
 * <code>ERROR(WAKE_ME_UP_RIGHT_NOW)</code> (ALERT), so that grep for <code>ERROR</code> still finds them.
 */
public class AiLevelConverter extends ClassicConverter {

    private static final String ERROR_PREFIX = "ERROR_";

    @Override
    public String convert(ILoggingEvent event) {
        return levelOf(event);
    }

    public static String levelOf(ILoggingEvent event) {
        String level = String.valueOf(event.getLevel());
        DevOpsMarker marker = devOpsMarker(event);
        if (marker == null || marker.getDevOpsLevel() == null) {
            return level;
        }
        DevOpsLevel devOpsLevel = marker.getDevOpsLevel();
        String name = devOpsLevel.name();
        return name.startsWith(ERROR_PREFIX) ? level + "(" + name.substring(ERROR_PREFIX.length()) + ")" : level;
    }

    /** The DevOps marker of the event, also if it refers to other markers or is referred to by one; null if there is none. */
    public static DevOpsMarker devOpsMarker(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        if (markers != null) {
            for (Marker marker : markers) {
                DevOpsMarker found = find(marker, 0);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static DevOpsMarker find(Marker marker, int depth) {
        if (marker instanceof DevOpsMarker devOpsMarker) {
            return devOpsMarker;
        }
        if (depth < 8 && marker.hasReferences()) {
            Iterator<Marker> iterator = marker.iterator();
            while (iterator.hasNext()) {
                DevOpsMarker found = find(iterator.next(), depth + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
