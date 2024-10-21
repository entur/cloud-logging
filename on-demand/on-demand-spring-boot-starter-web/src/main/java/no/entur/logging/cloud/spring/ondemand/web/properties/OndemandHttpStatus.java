package no.entur.logging.cloud.spring.ondemand.web.properties;

import java.util.ArrayList;
import java.util.List;

public class OndemandHttpStatus {

    private boolean enabled = true;
    private int equalOrHigherThan = -1;

    private List<Integer> equalTo = new ArrayList<>();

    private List<Integer> notEqualTo = new ArrayList<>();

    public int getEqualOrHigherThan() {
        return equalOrHigherThan;
    }

    public void setEqualOrHigherThan(int equalOrHigherThan) {
        this.equalOrHigherThan = equalOrHigherThan;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Integer> getEqualTo() {
        return equalTo;
    }

    public void setEqualTo(List<Integer> equalTo) {
        this.equalTo = equalTo;
    }

    public List<Integer> getNotEqualTo() {
        return notEqualTo;
    }

    public void setNotEqualTo(List<Integer> notEqualTo) {
        this.notEqualTo = notEqualTo;
    }
}
