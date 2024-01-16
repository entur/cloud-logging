package no.entur.logging.cloud.gcp.spring.ondemand.web.properties;

import java.util.ArrayList;
import java.util.List;

public class OndemandLogLevelTrigger {

    private boolean enabled = true;

    private String level = "error";

    private List<String> name = new ArrayList<>();

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
