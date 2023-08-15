package no.entur.logging.cloud.gcp.spring.web.properties;

import java.util.ArrayList;
import java.util.List;

public class OndemandHttpStatusCode {

    private int equalOrHigherThan = 400;

    private List<Integer> except = new ArrayList<>();

    public int getEqualOrHigherThan() {
        return equalOrHigherThan;
    }

    public void setEqualOrHigherThan(int equalOrHigherThan) {
        this.equalOrHigherThan = equalOrHigherThan;
    }

    public List<Integer> getExcept() {
        return except;
    }

    public void setExcept(List<Integer> except) {
        this.except = except;
    }
}
