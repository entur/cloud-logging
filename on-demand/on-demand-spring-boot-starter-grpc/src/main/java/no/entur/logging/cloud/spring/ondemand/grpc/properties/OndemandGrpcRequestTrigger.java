package no.entur.logging.cloud.spring.ondemand.grpc.properties;

import java.util.*;

public class OndemandGrpcRequestTrigger {

    private boolean enabled = true;

    private List<OndemandGrpcHeader> headers = new ArrayList<>();

    public List<OndemandGrpcHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<OndemandGrpcHeader> headers) {
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
        for (OndemandGrpcHeader header : headers) {
            if (header.isEnabled()) {
                headerNames.add(header.getName().toLowerCase());
            }
        }
        return headerNames;
    }

}
