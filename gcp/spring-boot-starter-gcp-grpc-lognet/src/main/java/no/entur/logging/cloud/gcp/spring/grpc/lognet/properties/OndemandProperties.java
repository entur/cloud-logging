package no.entur.logging.cloud.gcp.spring.grpc.lognet.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "no.entur.logging.grpc.ondemand")
public class OndemandProperties {

    private boolean enabled;

    private int interceptorOrder = Ordered.HIGHEST_PRECEDENCE;

    private OndemandSuccess success = new OndemandSuccess();

    private OndemandFailure failure = new OndemandFailure();

    private OndemandTroubleshoot troubleshoot = new OndemandTroubleshoot();

    private List<OndemandPath> paths = new ArrayList<>();

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

    public List<OndemandPath> getPaths() {
        return paths;
    }

    public void setPaths(List<OndemandPath> paths) {
        this.paths = paths;
    }

    public OndemandTroubleshoot getTroubleshoot() {
        return troubleshoot;
    }

    public void setTroubleshoot(OndemandTroubleshoot troubleshoot) {
        this.troubleshoot = troubleshoot;
    }

    public int getInterceptorOrder() {
        return interceptorOrder;
    }

    public void setInterceptorOrder(int interceptorOrder) {
        this.interceptorOrder = interceptorOrder;
    }
}