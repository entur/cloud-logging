package no.entur.logging.cloud.spring.ondemand.web.properties;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class OndemandFailure {

    private String level = "info";

    @NestedConfigurationProperty
    private OndemandHttpResponseTrigger http = new OndemandHttpResponseTrigger();

    @NestedConfigurationProperty
    private OndemandLogLevelTrigger logger = new OndemandLogLevelTrigger();

    @NestedConfigurationProperty
    private OndemandDurationTrigger duration = new OndemandDurationTrigger();

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

    public void setDuration(OndemandDurationTrigger duration) {
        this.duration = duration;
    }

    public OndemandDurationTrigger getDuration() {
        return duration;
    }
}
