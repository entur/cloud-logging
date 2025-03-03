package no.entur.logging.cloud.spring.ondemand.web.properties;

import java.time.Duration;

public class OndemandDurationTrigger {

    private boolean enabled = true;
    private Duration after;
    private Duration before;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setAfter(Duration after) {
        this.after = after;
    }

    public Duration getAfter() {
        return after;
    }

    public Duration getBefore() {
        return before;
    }

    public void setBefore(Duration before) {
        this.before = before;
    }
}
