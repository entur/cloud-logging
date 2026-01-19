package no.entur.logging.cloud.spring.ondemand.grpc.properties;

import no.entur.logging.cloud.appender.scope.LoggingScopeFlushMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "entur.logging.grpc.ondemand")
public class OndemandProperties {

    private boolean enabled;

    private int interceptorOrder = Ordered.HIGHEST_PRECEDENCE;

    private OndemandSuccess success = new OndemandSuccess();

    private OndemandFailure failure = new OndemandFailure();

    private OndemandTroubleshoot troubleshoot = new OndemandTroubleshoot();

    private List<OndemandPath> paths = new ArrayList<>();

    private LoggingScopeFlushMode flushMode = LoggingScopeFlushMode.EAGER;

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

    public LoggingScopeFlushMode getFlushMode() {
        return flushMode;
    }

    public void setFlushMode(LoggingScopeFlushMode flushMode) {
        this.flushMode = flushMode;
    }
}