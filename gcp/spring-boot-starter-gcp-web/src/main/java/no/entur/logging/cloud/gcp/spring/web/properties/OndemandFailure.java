package no.entur.logging.cloud.gcp.spring.web.properties;

public class OndemandFailure {

    private String level = "info";

    private OndemandHttpTrigger http = new OndemandHttpTrigger();
    private OndemandLogLevelTrigger logger = new OndemandLogLevelTrigger();

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public OndemandHttpTrigger getHttp() {
        return http;
    }

    public void setHttp(OndemandHttpTrigger http) {
        this.http = http;
    }

    public OndemandLogLevelTrigger getLogger() {
        return logger;
    }

    public void setLogger(OndemandLogLevelTrigger logger) {
        this.logger = logger;
    }
}
