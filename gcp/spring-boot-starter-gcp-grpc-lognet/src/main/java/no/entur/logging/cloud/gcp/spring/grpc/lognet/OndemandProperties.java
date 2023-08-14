package no.entur.logging.cloud.gcp.spring.grpc.lognet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "no.entur.logging.http.ondemand")
public class OndemandProperties {

    private boolean enabled;
    private String level = "warn";

    private int order = Ordered.HIGHEST_PRECEDENCE;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}