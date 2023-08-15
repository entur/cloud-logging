package no.entur.logging.cloud.gcp.spring.grpc.lognet.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "no.entur.logging.grpc.ondemand")
public class OndemandProperties {

    private boolean enabled;
    private OndemandSuccess success = new OndemandSuccess();

    private OndemandFailure failure = new OndemandFailure();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OndemandSuccess getSuccess() {
        return success;
    }

    public void setSuccess(OndemandSuccess success) {
        this.success = success;
    }

    public OndemandFailure getFailure() {
        return failure;
    }

    public void setFailure(OndemandFailure failure) {
        this.failure = failure;
    }
}