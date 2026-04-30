package no.entur.logging.cloud.logbook;

import net.logstash.logback.marker.SingleFieldAppendingMarker;

public abstract class RequestResponseSingleFieldAppendingMarker extends SingleFieldAppendingMarker  {

    protected final int truncated;

    public RequestResponseSingleFieldAppendingMarker(String markerName, String fieldName, int truncated) {
        super(markerName, fieldName);
        this.truncated = truncated;
    }

    public boolean isTruncated() {
        return truncated != -1;
    }

    /**
     * Number of bytes or chars which were truncated
     *
     * @return count
     */

    public int getTruncated() {
        return truncated;
    }
}
