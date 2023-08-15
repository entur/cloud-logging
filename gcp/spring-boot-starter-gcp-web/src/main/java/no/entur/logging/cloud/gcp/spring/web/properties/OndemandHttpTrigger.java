package no.entur.logging.cloud.gcp.spring.web.properties;

public class OndemandHttpTrigger {

    private boolean enabled = true;

    private OndemandHttpStatusCode statusCode = new OndemandHttpStatusCode();

    public void setStatusCode(OndemandHttpStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public OndemandHttpStatusCode getStatusCode() {
        return statusCode;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
