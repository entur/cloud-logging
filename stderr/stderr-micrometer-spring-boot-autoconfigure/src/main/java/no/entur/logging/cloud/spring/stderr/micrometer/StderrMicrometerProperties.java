package no.entur.logging.cloud.spring.stderr.micrometer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "entur.logging.stderr.micrometer")
public class StderrMicrometerProperties {

    /** Whether to intercept System.err output and update Micrometer counters. */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
