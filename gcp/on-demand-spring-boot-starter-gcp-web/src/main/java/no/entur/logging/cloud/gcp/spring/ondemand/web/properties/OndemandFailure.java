package no.entur.logging.cloud.gcp.spring.ondemand.web.properties;

public class OndemandFailure {

    private String level = "info";

    private OndemandHttpResponseTrigger http = new OndemandHttpResponseTrigger();
    private OndemandLogLevelTrigger logger = new OndemandLogLevelTrigger();

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public OndemandHttpResponseTrigger getHttp() {
        return http;
    }

    public void setHttp(OndemandHttpResponseTrigger http) {
        this.http = http;
    }

    public OndemandLogLevelTrigger getLogger() {
        return logger;
    }

    public void setLogger(OndemandLogLevelTrigger logger) {
        this.logger = logger;
    }
}
