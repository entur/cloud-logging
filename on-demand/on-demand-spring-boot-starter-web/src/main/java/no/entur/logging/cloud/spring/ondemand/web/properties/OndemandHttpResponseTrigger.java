package no.entur.logging.cloud.spring.ondemand.web.properties;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class OndemandHttpResponseTrigger {

    private boolean enabled = true;

    @NestedConfigurationProperty
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
