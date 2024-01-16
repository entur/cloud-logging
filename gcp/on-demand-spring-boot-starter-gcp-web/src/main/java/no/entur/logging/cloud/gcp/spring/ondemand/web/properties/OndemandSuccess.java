package no.entur.logging.cloud.gcp.spring.ondemand.web.properties;

public class OndemandSuccess {

    private String level = "warn";

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }
}
