package no.entur.logging.cloud.spring.ondemand.web.properties;

import java.util.*;

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

    public Set<String> toEnabledHeaderNames() {
        if(headers.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> headerNames = new HashSet<>(); // thread safe for reading
        for (OndemandHttpHeader header : headers) {
            if (header.isEnabled()) {
                headerNames.add(header.getName().toLowerCase());
            }
        }
        return headerNames;
    }

}
