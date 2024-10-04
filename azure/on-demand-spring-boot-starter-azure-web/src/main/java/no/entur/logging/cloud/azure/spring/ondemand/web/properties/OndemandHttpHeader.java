package no.entur.logging.cloud.azure.spring.ondemand.web.properties;

import java.util.ArrayList;
import java.util.List;

public class OndemandHttpHeader {

    private boolean enabled = true;
    private String name;

    public String getName() {
        return name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
