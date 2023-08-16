package no.entur.logging.cloud.gcp.spring.web.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "no.entur.logging.http.ondemand")
public class OndemandProperties {

    private boolean enabled;
    private OndemandSuccess success = new OndemandSuccess();

    private OndemandFailure failure = new OndemandFailure();

    private int filterOrder = Ordered.HIGHEST_PRECEDENCE;

    private String filterUrlPatterns = "/*";

    private List<OndemandPath> paths = new ArrayList<>();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setFilterOrder(int filterOrder) {
        this.filterOrder = filterOrder;
    }

    public int getFilterOrder() {
        return filterOrder;
    }

    public void setPaths(List<OndemandPath> paths) {
        this.paths = paths;
    }

    public List<OndemandPath> getPaths() {
        return paths;
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

    public void setFilterUrlPatterns(String filterUrlPatterns) {
        this.filterUrlPatterns = filterUrlPatterns;
    }

    public String getFilterUrlPatterns() {
        return filterUrlPatterns;
    }
}