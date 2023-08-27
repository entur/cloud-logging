package no.entur.logging.cloud.gcp.spring.web.properties;

public class OndemandHttpTrigger {

    private boolean enabled = true;

    private OndemandHttpStatus statusCode = new OndemandHttpStatus();


    public void setStatusCode(OndemandHttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public OndemandHttpStatus getStatusCode() {
        return statusCode;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
