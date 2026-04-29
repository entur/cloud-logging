package no.entur.logging.cloud.logbook;

import net.logstash.logback.marker.SingleFieldAppendingMarker;

public abstract class RequestResponseSingleFieldAppendingMarker extends SingleFieldAppendingMarker  {

    protected final boolean truncated;

    public RequestResponseSingleFieldAppendingMarker(String markerName, String fieldName, boolean truncated) {
        super(markerName, fieldName);
        this.truncated = truncated;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
