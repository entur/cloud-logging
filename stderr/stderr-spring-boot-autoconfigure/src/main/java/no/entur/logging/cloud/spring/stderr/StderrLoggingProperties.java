package no.entur.logging.cloud.spring.stderr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "entur.logging.stderr")
public class StderrLoggingProperties {

    /** Whether to forward System.err output to SLF4J. */
    private boolean enabled = true;

    /** SLF4J log level to use when forwarding System.err output. */
    private String level = "error";

    /** Logger name used when forwarding System.err output. */
    private String logger = "stderr";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }
}
