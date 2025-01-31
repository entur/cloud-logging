package no.entur.logging.cloud.spring.ondemand.web.properties;

public class OndemandDurationTrigger {

    private boolean enabled = false;
    private long milliseconds = -1;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setMilliseconds(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public long getMilliseconds() {
        return milliseconds;
    }
}
