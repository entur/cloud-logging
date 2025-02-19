package no.entur.logging.cloud.spring.ondemand.web.properties;

import no.entur.logging.cloud.appender.scope.LoggingScopeFlushMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "entur.logging.http.ondemand")
public class OndemandProperties {

    private boolean enabled;
    private OndemandSuccess success = new OndemandSuccess();

    private OndemandFailure failure = new OndemandFailure();

    private OndemandTroubleshoot troubleshoot = new OndemandTroubleshoot();

    public OndemandProperties() {
        // set default value
        failure.getHttp().getStatusCode().setEqualOrHigherThan(400);
    }

    private int filterOrder = Ordered.HIGHEST_PRECEDENCE;

    private String filterUrlPatterns = "/*";

    private List<OndemandPath> paths = new ArrayList<>();

    private LoggingScopeFlushMode flushMode = LoggingScopeFlushMode.EAGER;

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

    public OndemandTroubleshoot getTroubleshoot() {
        return troubleshoot;
    }

    public void setTroubleshoot(OndemandTroubleshoot troubleshoot) {
        this.troubleshoot = troubleshoot;
    }

    public LoggingScopeFlushMode getFlushMode() {
        return flushMode;
    }

    public void setFlushMode(LoggingScopeFlushMode flushMode) {
        this.flushMode = flushMode;
    }
}