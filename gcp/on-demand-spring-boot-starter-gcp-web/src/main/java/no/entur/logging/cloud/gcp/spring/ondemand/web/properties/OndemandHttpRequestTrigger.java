package no.entur.logging.cloud.gcp.spring.ondemand.web.properties;

import java.util.ArrayList;
import java.util.List;

public class OndemandHttpRequestTrigger {

    private boolean enabled = true;

    private List<OndemandHttpHeader> headers = new ArrayList<>();

    public List<OndemandHttpHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<OndemandHttpHeader> headers) {
        this.headers = headers;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
