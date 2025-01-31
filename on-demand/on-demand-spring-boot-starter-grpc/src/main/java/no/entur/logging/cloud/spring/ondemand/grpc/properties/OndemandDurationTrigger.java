package no.entur.logging.cloud.spring.ondemand.grpc.properties;

import java.time.Duration;

public class OndemandDurationTrigger {

    private boolean enabled = true;
    private Duration after;

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
}