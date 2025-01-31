package no.entur.logging.cloud.spring.ondemand.grpc.properties;

public class OndemandDurationTrigger {

    private boolean enabled = true;
    private String after;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getAfter() {
        return after;
    }
}